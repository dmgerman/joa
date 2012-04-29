SET statement_timeout = 0;
SET client_encoding = 'Latin1';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;
SET default_with_oids = false;

CREATE TABLE files (
    path character varying(255),
    basename character varying(255),
    suffix character varying(100),
    level integer,
    infilesha1 character(40),
    filesha1 character(40),
    uniqjavasigre integer,
    uniqclasssigre integer,
    java integer,
    class integer
);
COPY files (path, basename, suffix, level, infilesha1, filesha1, uniqjavasigre, uniqclasssigre, java, class) FROM stdin with delimiter ';';
