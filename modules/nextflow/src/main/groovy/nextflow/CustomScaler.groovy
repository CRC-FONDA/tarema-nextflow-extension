package nextflow

import groovy.sql.Sql
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.javatuples.Pair

import java.util.stream.Collectors

class CustomScaler {

    public static Integer getAction(String taskName, String containerName) {

        def sql = new Sql(TaskDB.getDataSource())

        def searchSql = 'SELECT task_name, cpus, realtime, memory from taskrun where task_name = \'' + taskName.split(" ")[0] + '\' and wf_name = \'' + containerName + '\''
        def results = sql.rows(searchSql)

        List<ResourceScale.HistoricalTask> taskList = new ArrayList<>();

        results.forEach( row -> {
            ResourceScale.HistoricalTask historicalTask = new ResourceScale.HistoricalTask()
            historicalTask.setCpus(row.get("cpus") as Integer)
            historicalTask.setRealtime(row.get("realtime") as Long)
            historicalTask.setMemory(row.get("memory") as Long)
            taskList.add(historicalTask)
        })

        return calculateCPUScale(sql,taskList, taskName.split(" ")[0])

    }

    private static Long calculateCPUScale(Sql sql, List<ResourceScale.HistoricalTask> list, String taskName) {

        def searchSql = 'SELECT * from taskactionreward where task_name = \'' + taskName.split(" ")[0] + '\''
        def rewards = sql.rows(searchSql)

        if(rewards.size() == 0) {

        }

        double step_size = 0.1

        //Pair : Action -> (H_value, probability)
        Map<Integer, Pair<Double, Double>> h = new HashMap<>();
        h.put(1,new Pair<Double, Double>(0.0,0.0))
        h.put(2,new Pair<Double, Double>(0.0,0.0))
        h.put(3,new Pair<Double, Double>(0.0,0.0))
        h.put(4,new Pair<Double, Double>(0.0,0.0))
        h.put(5,new Pair<Double, Double>(0.0,0.0))



        def probabilities = h.keySet().stream().map( key -> {
            double probability =  Math.exp(h.get(key).value0) / h.values().stream().map( val ->Math.exp(val.value0)).mapToDouble(Double::doubleValue).sum()
            h.put(key, new Pair<Double, Double>(h.get(key).value0, probability))
            return new org.apache.commons.math3.util.Pair<Integer, Double>(key, probability)
        }).collect(Collectors.toList())

        def distribution = new EnumeratedDistribution<Integer>(probabilities)
        Integer selectedAction = distribution.sample()


        Pair<Double, Double> r_values = calculateReward(selectedAction, list)


        // TODO r_t für t=t soll ignoriert werden und einen Sonderfall für 1
        h.keySet().stream().forEach(key -> {
            if(selectedAction == key) {
                return h.put(key, h.get(key).value0 + step_size * (r_values.value0 - r_values.value1) * (1 - h.get(key).value1))
            } else {
                h.put(key, h.get(key).value0 - step_size * (r_values.value0 - r_values.value1) * h.get(key).value1)
            }
        })
        // store h_t in table

        def qry = 'INSERT INTO taskactionreward (task_name, action, h_value, probability) VALUES (?,?) ' +
                'ON CONFLICT (task_name, action) DO UPDATE ' +
                'SET h_value = excluded.h_value ' +
                'SET probability = excluded.probability'
        sql.withBatch(5, qry) { ps ->
            h.forEach( entry -> {
                ps.addBatch(taskName, entry, h.get(entry).value0, h.get(entry).value1)
            })

        }

        return selectedAction
    }

}
