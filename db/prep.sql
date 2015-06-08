SHOW CLIENT_ENCODING;

SET CLIENT_ENCODING TO 'Latin1';

SHOW CLIENT_ENCODING;

\encoding latin1;

SHOW CLIENT_ENCODING;

SELECT NOW();
CREATE INDEX x11 ON sigs (filesha1);
SELECT NOW();
CREATE INDEX x12 ON sigs (sigsha1re);
SELECT NOW();
CREATE INDEX x13 ON sigs (losesigsha1re) ;

SELECT NOW();
CREATE INDEX x21 ON files (filesha1);
SELECT NOW();
CREATE INDEX x22 ON files (infilesha1);
SELECT NOW();
CREATE INDEX x23 ON files (basename VARCHAR_PATTERN_OPS);

SELECT NOW();
 
-- Update *.java tallies.
UPDATE files SET uniqjavasigre = tally FROM (
  SELECT
    COUNT(DISTINCT sigs.sigsha1re) AS tally, infilesha1
  FROM
    files NATURAL JOIN sigs WHERE suffix = '.java'
  GROUP BY
    infilesha1
) t
WHERE files.filesha1 = t.infilesha1 and files.uniqjavasigre is NULL;


SELECT NOW();

-- Update *.class tallies.
UPDATE files SET uniqclasssigre = tally FROM (
  SELECT
    COUNT(DISTINCT sigs.sigsha1re) AS tally, infilesha1
  FROM
    files NATURAL JOIN sigs WHERE suffix = '.class'
  GROUP BY
    infilesha1
) t
WHERE files.filesha1 = t.infilesha1 and files.uniqclasssigre IS NULL;

SELECT NOW();
SELECT COUNT(*) FROM files;
SELECT NOW();
SELECT COUNT(*) FROM sigs;
SELECT NOW();
