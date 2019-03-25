#!/usr/bin/env groovy

@Grab("org.twitter4j:twitter4j-core")

import twitter4j.Status
import twitter4j.Twitter
import twitter4j.TwitterFactory

static void main (String[] args) {
    Twitter twitter = TwitterFactory.getSingleton();
    Status status = twitter.updateStatus(args[0]);
    System.out.println("Successfully updated the status to [" + status.getText() + "].");
}