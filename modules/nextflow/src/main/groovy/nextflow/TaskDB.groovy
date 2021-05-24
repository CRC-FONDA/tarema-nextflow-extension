package nextflow

import com.zaxxer.hikari.HikariDataSource

import javax.sql.DataSource

class TaskDB {


    private static HikariDataSource ds = new HikariDataSource();

    private TaskDB() {

    }

    static {
        ds.setJdbcUrl("jdbc:postgresql://<>:<>/test");
        ds.setUsername("username");
        ds.setPassword("password");
        ds.setDriverClassName('org.postgresql.Driver')
    }

    public static DataSource getDataSource() {
        return ds
    }


}
