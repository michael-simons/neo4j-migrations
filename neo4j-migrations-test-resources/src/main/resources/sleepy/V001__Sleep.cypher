WITH apoc.date.currentTimestamp() AS start
CALL apoc.util.sleep(4000)
WITH start, apoc.date.currentTimestamp() AS end
RETURN datetime({epochmillis: start}) AS start,
       datetime({epochmillis: end}) AS end;
