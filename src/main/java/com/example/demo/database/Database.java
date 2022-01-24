package com.example.demo.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Se utilizeaza o clasa Singleton pentru a realiza conexiunea cu baza de date
public class Database {
    private static Database inst = null;

    public Connection conn;
    public String db_name;
    public String name;
    public String location;
    public String country;
    public String cost;
    public String start;
    public String end;

    private Database(String path, String db_name, String name, String country,
                     String location, String cost, String start,
                     String end) throws SQLException {
        this.conn = DriverManager.getConnection(path);
        this.db_name = db_name;
        this.name = name;
        this.country = country;
        this.location = location;
        this.cost = cost;
        this.start = start;
        this.end = end;
    }

    public static Database getInstance() throws SQLException {
        if (inst == null)
            inst = new Database("jdbc:sqlite:/home/onur/Desktop/Facultate/" +
                    "anul3/semestrul2/atta/data.db", "sports","name", "country",
                    "location", "cost", "start_date", "end_date");

        return inst;
    }
}
