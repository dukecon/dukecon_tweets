#!/usr/bin/env groovy
@Grab('com.xlson.groovycsv:groovycsv:1.1')
@GrabExclude('org.codehaus.groovy:groovy-all')

import static com.xlson.groovycsv.CsvParser.parseCsv
import groovy.json.JsonSlurper

int MAX_TITLE_LENGTH = 55

File twitterhandles = new File("twitterhandles.csv")
def csv = parseCsv(new FileReader(twitterhandles))

def twitterHandle = [:]
def twitterByEvent = [:]
csv.each { line ->
    String speakerName = line.Speaker.trim()
    if (twitterHandle[speakerName]) {
        System.err.println("Duplicate Speaker in CSV: ${speakerName}")
    } else if (line.TwitterHandle.startsWith("@")) {
        System.err.println("Speaker '${speakerName}' has TwitterHandle: '${line.TwitterHandle}'")
        twitterHandle[speakerName] = line.TwitterHandle
    } else {
        System.err.println("Speaker '${speakerName}' has no valid TwitterHandle!")
    }

    String eventId = line.EventId.trim()
    if (twitterByEvent[eventId]) {
        System.err.println("Duplicate EventId in CSV: ${eventId} (old: ${twitterByEvent[eventId]} (new: ${line})")
    } else if (line.TwitterHandle.startsWith("@")) {
        System.err.println("Event '${eventId}' has TwitterHandle: '${line.TwitterHandle}'")
        twitterByEvent[eventId] = line.TwitterHandle
    } else {
        System.err.println("Event '${eventId}' has no valid TwitterHandle!")
    }
}

// URL javalandContent = new URL ("https://dukecon.org/javaland/rest/conferences/499959")
// Retrieve via 'wget https://dukecon.org/javaland/rest/conferences/499959 -O javaland.json'
File javalandContentFile = new File("javaland.json")
//String javalandContent = javalandContentFile.getText('UTF-8').replaceAll("\\\\r", "")

//System.err.println ("JSON: ${javalandContent}")
JsonSlurper javalandSlurper = new JsonSlurper()
def javaland = javalandSlurper.parse(javalandContentFile, "ISO-8859-1") // javalandContent.bytes, 'UTF-8')

def speakers = javaland.speakers
def events = javaland.events

def eventById = [:]

events.each { event ->
    if (eventById[event.id]) {
        System.err.println("Duplicate Event '${event.id}'")
    } else {
        eventById[event.id] = event
    }
}

File twitterdata = new File("TwitterData.csv")
twitterdata << "Sprecher;TwitterHandle;EventTitle;EventId;Abstract;Tweet\n"
speakers.each { def speaker ->
    System.err.println("Speaker: ${speaker}")
    speaker.eventIds.each { def eventId ->
        String speakerName = speaker.name // .trim()
        String handle = twitterHandle[speakerName]
        if (!handle) {
            System.err.println("Speaker '${speakerName}' has no twitter handle (trying to find by event id)!")
            handle = twitterByEvent[eventId]
            if (!handle) {
                System.err.println("Speaker '${speakerName}' still has no twitter handle (skipping)")
                return
            }
        }

        System.err.println("Speaker '${speakerName}' / ${handle}: ${eventId}")
        def event = eventById[eventId]
        String eventUrl = "https://dukecon.org/javaland/talk.html#talk?talkId=$eventId"
        String title = event.title.replaceAll("(ausgebucht)", "").trim()
        int maxTitleLength = title.length() + handle.length()
        String titleShort = title
        if (maxTitleLength > MAX_TITLE_LENGTH) {
            titleShort = "${title.substring(0, MAX_TITLE_LENGTH - 3 - handle.length())}..."
            System.err.println("Shortened title '${title}' to '${titleShort}'")
        }
        String tweet = "Join ${handle} on '${titleShort}' @JavaLandConf: ${eventUrl}"
        if (tweet.length() >= 140) {
            System.err.println("Tweet is too long (${tweet.length()}): '${tweet}'")
        }
        def data = [
                speakerName, // Speaker
                handle, // TwitterHandle
                title, // Event Title
                eventId, // Event Id
                eventUrl, // Abstract
                tweet
        ]
        twitterdata << data.join(";") + "\n"
        println "sleep 5; ../scripts/TwitterSimpleUpdate.groovy \"${tweet}\""
    }
}
