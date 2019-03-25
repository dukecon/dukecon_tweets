#!/usr/bin/env groovy

@Grab("org.twitter4j:twitter4j-core")

import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterFactory

Twitter twitter = TwitterFactory.getSingleton();
List<Status> statuses = twitter.getHomeTimeline();
System.out.println("Showing home timeline.");
for (Status status : statuses) {
    System.out.println(status.getUser().getName() + ":" +
            status.getText());
}