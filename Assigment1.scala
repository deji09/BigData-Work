//The Folder that all the Data is stored in 
var initfolder = spark.sparkContext.textFile("SeasonData/*").mapPartitions(_.drop(1))
//This code maps the home team goals by their names and the goals scored by them per game 
val home = initfolder.map ( x => (x.split(",")(2),x.split(",")(4).toInt))
//This code maps the away team goals by their names and the goals scored by them per game
val away = initfolder.map ( x => (x.split(",")(3),x.split(",")(5).toInt))
//Calculate the total amount of goals by combining the home goals and away goals and merging the ones with the same team names. 
val finalGoal = (home union away).reduceByKey(_ + _)
//Prints the final result to a file and then to the terminal 
finalGoal.coalesce(1, shuffle = true).saveAsTextFile("Results")
finalGoal.foreach(println)
