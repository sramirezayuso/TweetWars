package it.itba.pod.tweetwars.player;

import ar.edu.itba.pod.mmxivii.tweetwars.GamePlayer;
import ar.edu.itba.pod.mmxivii.tweetwars.Status;
import ar.edu.itba.pod.mmxivii.tweetwars.TweetsProvider;
import ar.edu.itba.pod.mmxivii.tweetwars.impl.TweetsProviderImpl;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.rmi.RemoteException;

public class TweetBroadcaster implements Runnable
{
    private TweetsProvider tweetsProvider;
    private GamePlayer player;
    private String hash;
    private JChannel channel;

    public TweetBroadcaster(TweetsProvider tweetsProvider, GamePlayer player, String hash, JChannel channel)
    {
        this.tweetsProvider = tweetsProvider;
        this.player = player;
        this.hash = hash;
        this.channel = channel;
    }

    @Override
    public void run()
    {
        try
        {
            for (Status tweet : tweetsProvider.getNewTweets(player, hash, TweetsProviderImpl.MAX_BATCH_SIZE))
            {
                channel.send(new Message(null, null, tweet));
            }
        }
        catch (RemoteException e)
        {
            System.err.println("Remote communication error while fetching Tweets");
        }
        catch (Exception e)
        {
            System.err.println("Probably failed to send message to cluster: " + e.getMessage());
        }
    }

}
