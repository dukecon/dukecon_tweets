#!/usr/bin/env groovy
import groovy.json.JsonSlurper

final String year = "2019"
final def talkDaysRe = /.*2019-03-(19|20).*/
final String baseUrl = "https://programm.javaland.eu/${year}/#/scheduledEvent/"
final String hashTag = "#Javaland${year}"

void printUsageAndExit(int exitCode = 0, PrintStream out = System.out) {
    String usage =
            """
// tag::usage[]
usage: CreateTwitterPresentationPromotion -h | talks-data.json
// end::usage[]
"""

    out.println(usage.replaceAll(/\/\/.*\n/, ''))
    System.exit(exitCode)
}

if (args.length < 1) {
    System.err.println("Missing argument")
    printUsageAndExit(1, System.err)
} else if (args.length > 1) {
    System.err.println("To many arguments")
    printUsageAndExit(1, System.err)
} else if (args[0].equals("-h")) {
    printUsageAndExit(0, System.out)
}

JsonSlurper jsonSlurper = new JsonSlurper()
File talksFile = new File(args[0])
def talks = jsonSlurper.parse(talksFile, "UTF-8")

Map<String, Object> speakers = talks.speakers.collectEntries { speaker -> [(speaker.id): speaker]
}

Map<String, Object> events = talks.events.collectEntries { event -> [(event.id): event]
}

//speakers.values().sort {speaker -> speaker.lastname}.each {speaker ->
events.each { id, event ->
    if (Integer.valueOf(event.trackId ?: "100") <= 13 && event.start ==~ talkDaysRe) {
        def speakerIds = event.speakerIds
        def speakerInfos = speakerIds.findResults { speakerId ->
            def speaker = speakers[speakerId]
            if (speaker.twitter) {
                def twitter = speaker.twitter
                        .replaceAll("^(@|https?://(www\\.|)twitter.com/|http://|twitter.com/)", "")
                        .replaceAll("/\$", "")
                        .replaceAll("\\?.+", "")
                if (twitter) {
                    return "${speaker.name} (@${twitter})"
                }
                return null
            }
        }

        if (speakerInfos && (event.documents?.slides || event.documents?.manuscript || event.documents?.other)) {
            print "sleep 5; ../scripts/TwitterSimpleUpdate.groovy "
            if (event.languageId == "1") {
                def speakerInfo = speakerInfos.join(" und ")
                println "\"${speakerInfo} ${speakerInfos.size() > 1 ? "haben" : "hat"} Unterlagen zum @JavaLandConf Vortrag »${event.title.replaceAll("\"", "")}« hinterlegt: ${baseUrl}${id} ${hashTag}\""
            } else if (event.languageId == "2")  {
                def speakerInfo = speakerInfos.join(" and ")
                println "\"${speakerInfo} ${speakerInfos.size() > 1 ? "have" : "has"} uploaded a presentation for the @JavaLandConf talk »${event.title.replaceAll("\"", "")}«: ${baseUrl}${id} ${hashTag}\""
            }
        }
    }
}

