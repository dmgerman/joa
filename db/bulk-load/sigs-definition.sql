SET statement_timeout = 0;
SET client_encoding = 'latin1';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;
SET default_with_oids = false;

CREATE TABLE sigs (
    filesha1 character(40),
    classname character varying(1024),
    sigsha1re character(40),
    losesigsha1re character(40)
);
COPY sigs (filesha1, classname, sigsha1re, losesigsha1re) FROM stdin with delimiter ';';
