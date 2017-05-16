CREATE KEYSPACE IF NOT EXISTS groundvarsgrid WITH REPLICATION = {
    'class' : 'SimpleStrategy', 'replication_factor' : 1
};

USE groundvarsgrid;

CREATE TABLE IF NOT EXISTS variables (
    depth DOUBLE,
    lat DOUBLE,
    lon DOUBLE,
    oc DOUBLE,
    PRIMARY KEY(depth, lat, lon)
);