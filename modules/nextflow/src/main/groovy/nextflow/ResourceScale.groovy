package nextflow

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import nextflow.TaskDB
import org.apache.commons.math3.stat.StatUtils
import org.javatuples.Pair
import org.javatuples.Triplet

import java.util.stream.Collectors

@Slf4j
class ResourceScale {

    public static Integer getAction(String taskName, String containerName) {

        def sql = new Sql(TaskDB.getDataSource())

        def searchSql = 'SELECT task_name, cpus, realtime, memory from taskrun where task_name = \'' + taskName.split(" ")[0] + '\' and wf_name = \'' + containerName + '\''
        def results = sql.rows(searchSql)

        List<HistoricalTask> taskList = new ArrayList<>();

        results.forEach( row -> {
            HistoricalTask historicalTask = new HistoricalTask()
            historicalTask.setCpus(row.get("cpus") as Integer)
            historicalTask.setRealtime(row.get("realtime") as Long)
            historicalTask.setMemory(row.get("memory") as Long)
            taskList.add(historicalTask)
        })
        // Name ist falsch
        return calculateCPUScale(taskList, taskName.split(" ")[0])

    }

    private static Long calculateCPUScale(List<HistoricalTask> list) {

        def actionSpace = [1, 2, 3, 4, 5]

        Map<Integer, Integer> actionCalls = new HashMap<>()
        list.stream().forEach(item -> {
            if(actionCalls.get(item.cpus) !=null) {
                actionCalls.put(item.cpus, actionCalls.get(item.cpus) + 1)
            } else {
                actionCalls.put(item.cpus, 1)
            }
        })

        actionSpace.shuffle()
        def totalSteps = actionCalls.values().sum()

        def actionTriples = actionSpace.stream().map(action -> {

            def numberActionCalled = actionCalls.get(action)

            if(numberActionCalled == null) {
                log.info("numberActionCalled is null" + numberActionCalled)
                return new Triplet<Integer,Double, Double>(action, 1000.0, 0)
            }

            // TODO hier sollten wir die Werte
            // TODO die Arbeiten von Witt
            return new Triplet<Integer, Double, Double>(action, rewardForAction(action, list), 2 * Math.sqrt( (double) Math.log(totalSteps) / numberActionCalled))



        })/*.max( new Comparator<Pair<Integer, Double>>() {
            @Override
            int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {

                return Double.compare(o1.getValue1(), o2.getValue1())
            }
        }) */



        double[] d = actionTriples.map(trip -> trip.getValue1()).toArray(double[]::new)

        double normalizedData = StatUtils.normalize(d)

        // TODO map back
        // evtl line 67 collect o.Ä damit der Stream nicht mehrfach geöffnet


        return actionTriples.get().getValue0()
    }

    private static double rewardForAction(Integer action, ArrayList<HistoricalTask> list) {

                                                                                                                                                    // wieso hier -1???
        return list.stream().filter( task -> task.cpus.equals(action)).map(task -> (double) task.realtime * task.cpus * -1 ).mapToDouble(Double::doubleValue).average().orElse(0)

    }

    private static class HistoricalTask {

        String task_name;

        Double cpu;

        Long rss;

        Integer cpus;

        Long memory;

        Long realtime; // hier sollten wir auch wissen auf welcher Node oder?

    }

}
