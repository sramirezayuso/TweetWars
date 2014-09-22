package it.itba.pod.tweetwars.player;

import ar.edu.itba.pod.mmxivii.tweetwars.GameMaster;
import ar.edu.itba.pod.mmxivii.tweetwars.GamePlayer;
import ar.edu.itba.pod.mmxivii.tweetwars.TweetsProvider;
import org.jgroups.JChannel;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App
{
    public static final String TWEETS_PROVIDER_NAME = "tweetsProvider";
    public static final String GAME_MASTER_NAME = "gameMaster";
    public static final Random random = new Random();
    public static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    public static void main(String[] args)
    {
        final GamePlayer gamePlayer = new GamePlayer(args[3], "The player named " + args[3]);
        String clusterName = args[2];

        try
        {
            final Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
            final TweetsProvider tweetsProvider = (TweetsProvider) registry.lookup(TWEETS_PROVIDER_NAME);
            final GameMaster gameMaster = (GameMaster) registry.lookup(GAME_MASTER_NAME);

            final String hash = String.valueOf(random.nextLong());

            gameMaster.newPlayer(gamePlayer, hash);

            TweetReporter tweetReporter = new TweetReporter(gamePlayer, gameMaster, tweetsProvider);

            JChannel channel = new JChannel();
            channel.setReceiver(tweetReporter);
            channel.connect(clusterName);

            FakeTweetGenerator fakeTweetGenerator = new FakeTweetGenerator(gamePlayer, hash, channel);
            TweetBroadcaster tweetBroadcaster = new TweetBroadcaster(tweetsProvider, gamePlayer, hash, channel);

            scheduledExecutorService.scheduleWithFixedDelay(fakeTweetGenerator, 1l, 1l, TimeUnit.SECONDS);
            scheduledExecutorService.scheduleWithFixedDelay(tweetBroadcaster,1l, 1l, TimeUnit.SECONDS);
            scheduledExecutorService.scheduleWithFixedDelay(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        System.out.println("Current Score: " + gameMaster.getScore(gamePlayer));
                    }
                    catch (RemoteException e)
                    {
                        System.err.println("Remote communication error while fetching Score");
                    }
                }
            }, 5l, 5l, TimeUnit.SECONDS);
        }
        catch (RemoteException | NotBoundException e)
        {
            System.err.println("App Error: " + e.getMessage());
            System.exit(-1);
        }
        catch (Exception e)
        {
            System.err.println("Probably failed to connect to cluster: " + e.getMessage());
            System.exit(-1);
        }

    }
}
