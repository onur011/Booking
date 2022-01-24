package com.example.demo.controller;

import com.example.demo.database.Database;
import com.example.demo.sport.Sport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@RestController
public class Controller {
    private static final String STR_MIN_COST = "-1";
    private static final String STR_MAX_COST = "99999";
    private static final int MIN_COST = -1;
    private static final int MAX_COST = 99999;

    // Metoda de adaugare sport
    @PostMapping(path = "/add", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity addSport(@RequestBody Sport sport) throws SQLException {

        Database db = Database.getInstance();
        String str = "";
        Statement stmt;

        // Daca nu se respecta formatul json de adaugare
        if (sport.name == null || sport.country == null || sport.location == null ||
        sport.cost == MIN_COST || sport.start_date == null || sport.end_date == null) {
            str = "Adaugarea a esuat! Introduceti parametrii corecti.\n";
            return new ResponseEntity(str, HttpStatus.OK);
        }

        // Daca perioada este incorecta. start_date este mai mare decat end_date
        if (sport.start_date.compareTo(sport.end_date) > 0) {
            str = "Adaugarea a esuat! Perioada incorecta.\n";
            return new ResponseEntity(str, HttpStatus.OK);
        }

        // Se verifica daca exista deja in baza de date o inregistrare cu acelasi
        // nume, tara, locatie si perioada care se intersecteaza cu cea pe care
        // dorim sa o adaugam
        String query_select = "SELECT * FROM " + db.db_name +
        " WHERE " +
            db.name + " = '" + sport.name + "' AND " +
            db.country + " = '" + sport.country + "' AND " +
            db.location + " = '" + sport.location + "' AND (" +
            "strftime('%Y-%m-%d', '"+ sport.start_date+"') BETWEEN strftime('%Y-%m-%d', "+db.start+") AND strftime('%Y-%m-%d', "+ db.end+") OR " +
            "strftime('%Y-%m-%d', '"+ sport.end_date+"') BETWEEN strftime('%Y-%m-%d', "+db.start+") AND strftime('%Y-%m-%d', "+ db.end+") OR " +
            "( strftime('%Y-%m-%d', '"+ sport.start_date +"') <= strftime('%Y-%m-%d', "+db.start+") " +
            " AND " +
            "strftime('%Y-%m-%d', '"+sport.end_date+"') >= strftime" + "('%Y-%m-%d', "+db.end+")));";


        stmt = db.conn.createStatement();
		ResultSet result_select = stmt.executeQuery(query_select);

        //Daca exista conflict intre inregistrari, se afiseaza inregistrarile
        if (result_select.next()) {
            str = "Nu s-a putut adauga. Este in conflict cu:\n";

            do {
                str += result_select.getString(db.name) + " | " +
                        result_select.getString(db.country) + " | " +
                        result_select.getString(db.location) + " | " +
                        result_select.getString(db.cost)  + " | " +
                        result_select.getString(db.start) + " | " +
                        result_select.getString(db.end) + "\n";
            } while (result_select.next());

        } else {
            // Daca nu exista niciun conflict, se adauga inregistrarea in tabela
            String query_insert = "INSERT INTO " + db.db_name + " (" + db.name + "," +
                    db.country + "," + db.location + "," + db.cost + "," + db.start + "," +
                    db.end + ")" + " VALUES('" + sport.name + "','" + sport.country + "','" +
                    sport.location + "'," + sport.cost + ",'" + sport.start_date + "','" +
                    sport.end_date + "');";

            stmt.executeUpdate(query_insert);
            str = "A fost adaugat cu succes!\n";
        }
		return new ResponseEntity(str, HttpStatus.OK);
    }

    // Metoda de stergere
    @PostMapping(path = "/delete", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity deleteSport(@RequestBody Sport sport) throws SQLException {
        Database db = Database.getInstance();
        Statement stmt;
        String str = "";

        // Daca nu se respecta formatul json de stergere
        if (sport.name == null || sport.country == null || sport.location == null ||
                sport.cost == MIN_COST || sport.start_date == null || sport.end_date == null) {
            str = "Strgerea a esuat! Introduceti parametrii corecti.\n";
            return new ResponseEntity(str, HttpStatus.OK);
        }

        // Se face o cautare in tabela, pentru a stii daca inregistrarea exista
        String query_select = "SELECT * FROM " + db.db_name + " WHERE " +
                db.name + " = '" + sport.name + "' AND " +
                db.country + " = '" + sport.country + "' AND " +
                db.location + " = '" + sport.location + "' AND " +
                db.cost + " = " + sport.cost + " AND " +
                db.start + " = '" + sport.start_date + "' AND " +
                db.end + " = '" + sport.end_date + "';";

        stmt = db.conn.createStatement();
        ResultSet rs = stmt.executeQuery(query_select);

        // Daca inregistrarea exista, aceasta este stearsa
        if (rs.next()) {

            String query_delete = "DELETE FROM "+db.db_name + " WHERE " +
                "id = " + rs.getString("id") + ";";

            stmt.executeUpdate(query_delete);
            str = "Inregistrarea a fost stearsa cu succes!\n";

        } else {
            // Daca nu exista, se anunta ca nu s-a putut sterge
            str = "Stergerea a esuat! Nu exista in baza de date.\n";
        }
        return new ResponseEntity(str, HttpStatus.OK);
    }

    // Metoda de selectie
    @GetMapping("/select")
    public ResponseEntity selectSports(@RequestParam(required = false) List<String> name,
                                       @RequestParam(required = false) List<String> country,
                                       @RequestParam(required = false) List<String> location,
                                       @RequestParam(defaultValue = STR_MIN_COST) int cost_min,
                                       @RequestParam(defaultValue = STR_MAX_COST) int cost_max,
                                       @RequestParam(required = false) String start_date,
                                       @RequestParam(required = false) String end_date) throws SQLException {
        Database db = Database.getInstance();
        Statement stmt;
        String str = "";
        String query = "";

        // In cazul in care cos_min este mai mare decat cost_max
        if (cost_min != MIN_COST && cost_max != MAX_COST && (cost_min > cost_max)) {
            str = "Selectia a esuat! Interval cost incorect.\n";
            return new ResponseEntity(str, HttpStatus.OK);
        }

        // In cazul in care start_date este mai mare decat end_date
        if (start_date != null && end_date != null && (start_date.compareTo(end_date) > 0)) {
            str = "Selectia a esuat! Perioada incorecta.\n";
            return new ResponseEntity(str, HttpStatus.OK);
        }

        stmt = db.conn.createStatement();
        query = "SELECT * FROM " + db.db_name;

        // Se afiseaza toata baza de date, daca nu s-a introdus niciun parametru
        if (name == null && location == null && country == null && cost_min == MIN_COST &&
        cost_max == MAX_COST && start_date == null && end_date == null) {
            query += ";";
        } else {
        // Se selecteaza in functie de parametrii introdusi
            query += " WHERE ";
            boolean set_and = false;

            // Selectia dupa lista de nume
            if (name != null) {
                query += setQueryList(name, db.name);
                set_and = true;
            }

            // Selectia dupa lista de tari
            if (country != null) {
                if (set_and) {
                    query += " AND ";
                } else set_and = true;
                query += setQueryList(country, db.country);
            }

            // Selectia dupa lista de locatii
            if (location != null) {
                if (set_and) {
                    query += " AND ";
                } else set_and = true;
                query += setQueryList(location, db.location);
            }

            // Selectia dupa intervalul de cost
            if (cost_min != MIN_COST || cost_max != MAX_COST) {
                if (set_and) {
                    query += " AND ";
                } else set_and = true;

                query += setQueryCost(cost_min, cost_max, db.cost);
            }

            // Selectia dupa perioada
            if (start_date != null || end_date != null) {
                if (set_and) {
                    query += " AND ";
                }

                query += setQueryDate(start_date, end_date, db.start, db.end);
            }
        }
        query += ";";
        ResultSet rs = stmt.executeQuery(query);

        str = "";
        // Se afiseaza rezultatul selectie, daca exista
        if (rs.next()) {
            do {
            str += rs.getString(db.name) + " | " +
                rs.getString(db.country) + " | " +
                rs.getString(db.location) + " | " +
                rs.getString(db.cost) + " | " +
                rs.getString(db.start) + " | " +
                rs.getString(db.end) + "\n";
            } while(rs.next());
        } else {
            // Daca nu exista un rezultat al selectie, se anunta acest lucru
            str = "Nu exista inregistrari pentru selectia facuta.\n";
        }

        return new ResponseEntity(str, HttpStatus.OK);
    }

    // Metode de modificare
    @PostMapping(path = "/update", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity updateSport(@RequestBody Sport sport) throws SQLException {
        Database db = Database.getInstance();
        Statement stmt;
        String str = "";

        // Daca nu se respecta formatul json de modificare
        if((sport.name == null || sport.country == null || sport.location == null ||
        sport.cost == MIN_COST || sport.start_date == null || sport.end_date == null) ||
        (sport.new_name == null && sport.new_country == null && sport.new_location == null &&
        sport.new_cost == MIN_COST && sport.new_start_date == null &&
        sport.new_end_date == null)) {
            str = "Modificarea a esuat! Introduceti parametrii corecti.\n";
            return new ResponseEntity(str, HttpStatus.OK);
        }

        // Daca perioada introdusa nu este valida
        if ((sport.new_start_date != null && sport.new_end_date != null &&
        sport.new_start_date.compareTo(sport.new_end_date) > 0) ||
        (sport.new_start_date != null && sport.new_end_date == null &&
        sport.new_start_date.compareTo(sport.end_date) > 0) ||
        (sport.new_start_date == null && sport.new_end_date != null &&
        sport.start_date.compareTo(sport.new_end_date) > 0 )) {
            str = "Modificarea a esuat! Perioada incorecta.\n";
            return new ResponseEntity(str, HttpStatus.OK);
        }

        // Se face o selectie dupa inregistrarea care trebuie modificata
        String query_select = "SELECT * FROM " + db.db_name + " WHERE " +
                db.name + " = '" + sport.name + "' AND " +
                db.country + " = '" + sport.country + "' AND " +
                db.location + " = '" + sport.location + "' AND " +
                db.cost + " = " + sport.cost + " AND " +
                db.start + " = '" + sport.start_date + "' AND " +
                db.end + " = '" + sport.end_date + "';";

        stmt = db.conn.createStatement();
        ResultSet rs = stmt.executeQuery(query_select);
        String id = "";
        // Daca inregistrarea exista
        if(rs.next()) {

            // Se verifica daca facand modificarea, inregistrarea intra
            // in conflict cu alte inregistrari

            id = rs.getString("id");

            String query_verify = "SELECT * FROM " + db.db_name + " WHERE ";

            query_verify += "id != " + id + " AND ";

            if (sport.new_name != null) {
                query_verify += db.name + " = '" + sport.new_name + "' AND ";
            } else {
                query_verify += db.name + " = '" + sport.name + "' AND ";
            }

            if (sport.new_country != null) {
                query_verify += db.country + " = '" + sport.new_country + "' AND ";
            } else {
                query_verify += db.country + " = '" + sport.country + "' AND ";
            }

            if (sport.new_location != null) {
                query_verify += db.location + " = '" + sport.new_location + "' AND (";
            } else {
                query_verify += db.location + " = '" + sport.location + "' AND (";
            }

            if (sport.new_start_date != null) {
                query_verify += "strftime('%Y-%m-%d', '"+ sport.new_start_date+"') BETWEEN strftime('%Y-%m-%d', "+db.start+") AND strftime('%Y-%m-%d', "+ db.end+") OR ";
            } else {
                query_verify += "strftime('%Y-%m-%d', '"+ sport.start_date+"') BETWEEN strftime('%Y-%m-%d', "+db.start+") AND strftime('%Y-%m-%d', "+ db.end+") OR ";
            }

            if (sport.new_end_date != null) {
                query_verify += "strftime('%Y-%m-%d', '"+ sport.new_end_date+"') BETWEEN strftime('%Y-%m-%d', "+db.start+") AND strftime('%Y-%m-%d', "+ db.end+") OR ";
            } else {
                query_verify += "strftime('%Y-%m-%d', '"+ sport.end_date+"') BETWEEN strftime('%Y-%m-%d', "+db.start+") AND strftime('%Y-%m-%d', "+ db.end+") OR ";
            }

            if (sport.new_start_date != null) {
                query_verify += "( strftime('%Y-%m-%d', '"+ sport.new_start_date +"') <= strftime('%Y-%m-%d', "+db.start+")  AND ";
            } else {
                query_verify += "( strftime('%Y-%m-%d', '"+ sport.start_date +"') <= strftime('%Y-%m-%d', "+db.start+")  AND ";
            }

            if (sport.new_end_date != null) {
                query_verify += "strftime('%Y-%m-%d', '"+sport.new_end_date+"') >= strftime" + "('%Y-%m-%d', "+db.end+")));";
            } else {
                query_verify += "strftime('%Y-%m-%d', '"+sport.end_date+"') >= strftime" + "('%Y-%m-%d', "+db.end+")));";
            }

            ResultSet result_select = stmt.executeQuery(query_verify);

            // Daca intra in conflict, modificarea nu se efectueaza
            if (result_select.next()) {
                String str_select = "Modificarea a esuat! Este in conflict cu:\n";

                do {
                    str_select += result_select.getString(db.name) + " | " +
                            result_select.getString(db.country) + " | " +
                            result_select.getString(db.location) + " | " +
                            result_select.getString(db.cost)  + " | " +
                            result_select.getString(db.start) + " | " +
                            result_select.getString(db.end) + "\n";
                } while (result_select.next());

                return new ResponseEntity(str_select, HttpStatus.OK);
            }

            // Se efectueaza modificarea
            String query_update = "UPDATE "+db.db_name + " SET ";
            boolean set_dot = false;

            if (sport.new_name != null) {
                query_update += db.name + " = '" + sport.new_name + "'";
                set_dot = true;
            }

            if (sport.new_country != null) {
                if (set_dot) {
                    query_update += ", ";
                } else set_dot = true;

                query_update += db.country + " = '" + sport.new_country + "'";
            }

            if (sport.new_location != null) {
                if (set_dot) {
                    query_update += ", ";
                } else set_dot = true;

                query_update += db.location + " = '" + sport.new_location + "'";
            }

            if (sport.new_cost != MIN_COST) {
                if (set_dot) {
                    query_update += ", ";
                } else set_dot = true;

                query_update += db.cost + " = " + sport.new_cost;
            }

            if (sport.new_start_date != null) {
                if (set_dot) {
                    query_update += ", ";
                } else set_dot = true;

                query_update += db.start + " = '" + sport.new_start_date + "'";
            }

            if (sport.new_end_date != null) {
                if (set_dot) {
                    query_update += ", ";
                } else set_dot = true;

                query_update += db.end + " = '" + sport.new_end_date + "'";
            }

            query_update += " WHERE id = "  + id + ";";

            stmt.executeUpdate(query_update);
            str = "Inregistrarea a fost modificata cu succes!\n";
        } else {
            // Daca inregistrarea nu exista, se anunta
            str = "Modificarea a esuat! Inregistrarea nu exista.\n";
        }

        return new ResponseEntity(str, HttpStatus.OK);
    }

    // Functie de generare query pt elemente din lista
    public String setQueryList(List<String> lst, String cond) {
        String query = "";

        boolean set_or = false;
        query += "(";
        // Se adauga fiecare conditie, fiind legate prin AND
        for (String elem : lst) {
            if (set_or) {
                query += " OR ";
            }

            query += cond + " = '" + elem +"'";

            set_or = true;
        }
        query += ")";

        return query;
    }

    // Functie de generarea query pentru cost in interval [min,max]
    public String setQueryCost(int min, int max, String cost) {
        String query = "";

        query += "(";
        // Se adauga conditia min < cost < max
        if (min != MIN_COST && max != MAX_COST) {
            query += cost + " >= " + min + " AND " + cost + " <= " + max;

        // Se adauga conditia min < cost
        } else if (min != MIN_COST) {
            query += cost + " >= " + min;

        // Se adauga conditia cost < max
        } else if (max != MAX_COST) {
            query += cost + " <= " + max;
        }

        query += ")";

        return query;
    }

    // Functie generarea query pentru a selecta perioada dorita
    public String setQueryDate(String start, String end, String start_date, String end_date) {
        String query = "";

        query += "(";
        // Se aduaga conditi de exista a intersectie intre [start,end] si [start_date,end_date]
        if (start != null && end != null) {
            query +=
            "strftime('%Y-%m-%d', '"+ start+"') BETWEEN strftime('%Y-%m-%d', "+start_date+") AND strftime('%Y-%m-%d', "+ end_date+") OR " +
            "strftime('%Y-%m-%d', '"+ end+"') BETWEEN strftime('%Y-%m-%d', "+start_date+") AND strftime('%Y-%m-%d', "+ end_date+") OR " +
            "( strftime('%Y-%m-%d', '"+ start +"') <= strftime('%Y-%m-%d', "+start_date+") " +
            " AND " +
            "strftime('%Y-%m-%d', '"+ end+"') >= strftime" + "('%Y-%m-%d', "+end_date+"))";

        // Se adauga conditia ca start <= end_date  (se asigura exista intersectiei)
        } else if (start != null) {
            query += "strftime('%Y-%m-%d', '"+ start +"') <= strftime('%Y-%m-%d', "+end_date+")";

        // Se adauga conditia ca end >= start_date (se asigura exista intersectiei)
        } else if (end != null) {
            query += "strftime('%Y-%m-%d', '"+ end +"') >= strftime('%Y-%m-%d', "+start_date+")";
        }

        query += ")";

        return query;
    }
}
