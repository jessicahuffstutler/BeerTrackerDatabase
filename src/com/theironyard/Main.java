package com.theironyard;

import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import javax.xml.transform.Result;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    //Write a static method insertBeer
    static void insertBeer(Connection conn, String name, String type) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO beers VALUES (NULL, ?, ?)"); //need prepared statement since we are injecting values entered by the user
        //above (NULL ? ?) -> null is id, ? #1 = name, ? #2 = type;
        stmt.setString(1, name);
        stmt.setString(2, type);
        stmt.execute();
    }

    //Write a static method deleteBeer
    static void deleteBeer(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM beers WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    static void editBeer(Connection conn, int id, String name, String type) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE beers SET name = ?, type = ? WHERE id = ?");
        stmt.setString(1, name);
        stmt.setString(2, type);
        stmt.setInt(3, id);
        stmt.execute();
    }

    //Write a static method selectBeers that returns an ArrayList<Beer> containing all the beers in the database
    static ArrayList<Beer> selectBeers (Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery("SELECT * FROM beers");
        ArrayList<Beer> beers = new ArrayList();
        while (results.next()) {
            Beer beer = new Beer(); //creating the object
            beer.id = results.getInt("id");  //pull out the id from that column
            beer.name = results.getString("name");
            beer.type = results.getString("type");
            beers.add(beer);
        }
        return beers;
    }

    public static void main(String[] args) throws SQLException {
        //create the Connection
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        //execute a query to create a beers table
        stmt.execute("CREATE TABLE IF NOT EXISTS beers (id IDENTITY, name VARCHAR, type VARCHAR)"); //id column, name column, type column

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    ArrayList<Beer> beers = selectBeers(conn);
                    if (username == null) {
                        return new ModelAndView(new HashMap(), "not-logged-in.html");
                    }
                    HashMap m = new HashMap();
                    m.put("username", username);
                    m.put("beers", beers); //could be m.put("beers", selectBeers(conn); and we could remove the arrayList creation above.
                    return new ModelAndView(m, "logged-in.html");
                }),
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/login",
                ((request, response) -> {
                    String username = request.queryParams("username");
                    Session session = request.session();
                    session.attribute("username", username);
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post(
                "/create-beer",
                ((request, response) -> {
                    Beer beer = new Beer();
//                    beer.id = beers.size() + 1;
                    beer.name = request.queryParams("beername");
                    beer.type = request.queryParams("beertype");
                    insertBeer(conn, beer.name, beer.type);
//                    beers.add(beer);
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post( //only deletes beer # 1 when Beer # = 1 is entered. Will not delete any other #s
                "/delete-beer",
                ((request, response) -> {
                    String id = request.queryParams("beerid");
                    try {
                        int idNum = Integer.valueOf(id);
                        deleteBeer(conn, idNum);
//                        beers.remove(idNum-1);
//                        for (int i = 0; i < beers.size(); i++) {
//                            beers.get(i).id = i + 1;
//                        }
                    } catch (Exception e) {

                    }
                    response.redirect("/");
                    return "";
                })
        );
        Spark.post( //makes no updates when edit button is pressed
                "/edit-beer",
                ((request, response) -> {
                    String id = request.queryParams("beerid"); //pulling in 3 diff query parameters
                    String name = request.queryParams("editBeerName");
                    String type = request.queryParams("editBeerType");
                    try {
                        int idNum = Integer.valueOf(id);
                        editBeer(conn, idNum, name, type); //passing into the method
                    } catch (Exception e) {

                    }
                    response.redirect("/");
                    return "";
                })
        );
    }
}
