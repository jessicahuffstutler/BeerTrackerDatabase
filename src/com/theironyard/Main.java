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
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO beers VALUES (?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, type);
        stmt.execute();
    }

    //Write a static method deleteBeer
    static void deleteBeer (Connection conn, int idNum) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM beers WHERE ROWNUM = ?");
        stmt.setInt(1, idNum);
        stmt.execute();
    }

    static void editBeer (Connection conn, String name, String type, int idNum) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE beers SET (name = ?, type = ?) WHERE ROWNUM = ?");
        stmt.setString(1, name);
        stmt.setString(2, type);
        stmt.setInt(3, idNum);
        stmt.execute();
    }

    //Write a static method selectBeers that returns an ArrayList<Beer> containing all the beers in the database
    static ArrayList<Beer> selectBeers (Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        int id = 1;
        ResultSet results = stmt.executeQuery("SELECT * FROM beers");
        ArrayList<Beer> beers = new ArrayList();
        while (results.next()) {
            String name = results.getString("name");
            String type = results.getString("type");
            Beer drink = new Beer();
            drink.id = id; //sets numbering
            drink.name = name;
            drink.type = type;
            beers.add(drink);
            id++;
        }

        return beers;
    }

    public static void main(String[] args) throws SQLException {
        //create the Connection
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        //execute a query to create a beers table
        stmt.execute("CREATE TABLE IF NOT EXISTS beers (name VARCHAR, type VARCHAR)");

         //how to move this into the "/" route?
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
                    m.put("beers", beers);
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
                    String edit = request.queryParams("beerid");
                    try {
                        int idNum = Integer.valueOf(edit);
                        String name = request.queryParams("beername");
                        String type = request.queryParams("beertype");
                        editBeer(conn, name, type, idNum);
                    } catch (Exception e) {

                    }
                    response.redirect("/");
                    return "";
                })
        );
    }
}
