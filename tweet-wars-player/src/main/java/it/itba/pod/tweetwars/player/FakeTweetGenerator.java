package it.itba.pod.tweetwars.player;

import ar.edu.itba.pod.mmxivii.tweetwars.GamePlayer;
import ar.edu.itba.pod.mmxivii.tweetwars.Status;
import ar.edu.itba.pod.mmxivii.tweetwars.TweetsProvider;
import org.jgroups.JChannel;
import org.jgroups.Message;

import java.util.Random;

public class FakeTweetGenerator implements Runnable
{
    private Random random = new Random();

    private GamePlayer player;
    private String hash;
    private JChannel channel;
    private Long fakeID;

    public FakeTweetGenerator(GamePlayer player, String hash, JChannel channel)
    {
        this.player = player;
        this.hash = hash;
        this.channel = channel;
        this.fakeID = random.nextLong();
    }

    @Override
    public void run()
    {
        try
        {
            Status fakeTweet = new Status(fakeID, String.valueOf(random.nextLong()),  player.getId(), hash);
            channel.send(new Message(null, null, fakeTweet));
        }
        catch (Exception e)
        {
            System.err.println("Remote communication error while sending fake Tweet");
        }
    }

}
