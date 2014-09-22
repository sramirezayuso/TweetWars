package it.itba.pod.tweetwars.player;

import ar.edu.itba.pod.mmxivii.tweetwars.GameMaster;
import ar.edu.itba.pod.mmxivii.tweetwars.GamePlayer;
import ar.edu.itba.pod.mmxivii.tweetwars.Status;
import ar.edu.itba.pod.mmxivii.tweetwars.TweetsProvider;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TweetReporter extends ReceiverAdapter
{
    private GamePlayer player;
    private GameMaster gameMaster;
    private TweetsProvider tweetsProvider;
    private Map<String, List<Status>> fakeTweets;

    public TweetReporter(GamePlayer player, GameMaster gameMaster, TweetsProvider tweetsProvider)
    {
        this.player = player;
        this.gameMaster = gameMaster;
        this.tweetsProvider = tweetsProvider;
        this.fakeTweets = new HashMap<>();
    }

    @Override
    public void receive(Message msg)
    {
        System.out.println("Received: " + msg + "from" + ((Status) msg.getObject()).getSource());
        try
        {
            if (msg.getObject() instanceof Status)
            {
                Status receivedTweet = (Status) msg.getObject();
                if (!player.getId().equals(receivedTweet.getSource()))
                {
                    if (isFake(receivedTweet))
                    {
                        reportFake(receivedTweet);
                    } else
                    {
                        gameMaster.tweetReceived(player, receivedTweet);
                    }
                }
            }
        }
        catch (RemoteException e)
        {
            System.err.println("Remote communication error while processing received Tweet");
        }
    }

    private boolean isFake(Status tweet) throws RemoteException
    {
        Status realTweet = tweetsProvider.getTweet(tweet.getId());
        return (realTweet == null || !realTweet.equals(tweet));
    }

    private void reportFake(Status tweet) throws RemoteException
    {
        List<Status> playerFakeTweets = fakeTweets.get(tweet.getSource());
        if(playerFakeTweets == null)
        {
            playerFakeTweets = new ArrayList<>();
            fakeTweets.put(tweet.getSource(), playerFakeTweets);
        }
        playerFakeTweets.add(tweet);
        if (playerFakeTweets.size() >= GameMaster.MIN_FAKE_TWEETS_BATCH) {
            gameMaster.reportFake(player, playerFakeTweets.toArray(new Status[1]));
        }
    }
}
