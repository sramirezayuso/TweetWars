/*
* (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
* law. All use of this software is subject to MuleSoft's Master Subscription Agreement
* (or other master license agreement) separately entered into in writing between you and
* MuleSoft. If such an agreement is not in place, you may not use the software.
*/

package it.itba.pod.tweetwars.player;

import ar.edu.itba.pod.mmxivii.tweetwars.GameMaster;
import ar.edu.itba.pod.mmxivii.tweetwars.GamePlayer;
import ar.edu.itba.pod.mmxivii.tweetwars.Status;
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
        final GamePlayer gamePlayer = new GamePlayer(args[2], "The player named " + args[2]);
        String clusterName = args[1];

        try
        {
            final Registry registry = LocateRegistry.getRegistry(Integer.parseInt(args[0]));
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
                        System.out.println(gameMaster.getScore(gamePlayer));
                    }
                    catch (RemoteException e)
                    {
                        System.err.println("Remote communication error while fetching Score");
                    }
                }
            }, 1l, 1l, TimeUnit.SECONDS);
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

    /*public static void main(String[] args)
    {
        final GamePlayer gp = new GamePlayer("yo", "aquel");
        final GamePlayer gp2 = new GamePlayer("yo2", "aquel otro");
        System.out.println("empezando!");
        try {
            final Registry registry = LocateRegistry.getRegistry(args[0], 7242);
            final TweetsProvider tweetsProvider = (TweetsProvider) registry.lookup(TWEETS_PROVIDER_NAME);
            final GameMaster gameMaster = (GameMaster) registry.lookup(GAME_MASTER_NAME);

            final String hash = "abceddd";
            try {
                gameMaster.newPlayer(gp, hash);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            try {
                gameMaster.newPlayer(gp2, hash);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

            final Status[] tweets = tweetsProvider.getNewTweets(gp, hash, 10);
            for (Status tweet : tweets) {
                System.out.println("tweet = " + tweet);
                gameMaster.tweetReceived(gp2, tweet);
            }

            for (int i = 0; i < 10; i++) {
                System.out.println("new tweets " + i);
                final Status[] newTweets = tweetsProvider.getNewTweets(gp, hash, 100);
                gameMaster.tweetsReceived(gp2, newTweets);
            }

        } catch (RemoteException | NotBoundException e) {
            System.err.println("App Error: " + e.getMessage());
            System.exit(-1);
        }
        System.out.println("Hola alumno!");
    }*/
}
