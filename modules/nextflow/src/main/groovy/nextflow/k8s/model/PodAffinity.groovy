package nextflow.k8s.model

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j

@EqualsAndHashCode
@ToString(includeNames = true)
@CompileStatic
@Slf4j
class PodAffinity {

    def affinity

    PodAffinity(Map<String, String> labels) {

        def nodeAffinity = [
                preferredDuringSchedulingIgnoredDuringExecution: createAffinities(labels)
        ]

        this.affinity = [
                nodeAffinity: nodeAffinity
        ]
    }

    private  List<LinkedHashMap<String, Serializable>> createAffinities(Map<String, String> labels) {
        // TODO points for 10 and 5
        List listToReturn = []

        labels.forEach((key, value) -> {
            def values = [
                    value
            ]

            def exp = [
                    key     : key,
                    operator: "In",
                    values  : values
            ]

            def matchExpressions = [
                    exp
            ]

            def preference = [
                    matchExpressions: matchExpressions
            ]

            def wgth = 25

            if(value.contains("3")) {
                wgth = 100
            } else if (value.contains("2")) {
                wgth = 50
            }

            def some = [
                    weight    : wgth,
                    preference: preference
            ]

            listToReturn.add(some)
        })

        return listToReturn
    }
}
