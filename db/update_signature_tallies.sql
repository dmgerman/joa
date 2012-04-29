
-- NOTE:  files AND sigs MUST CONTAIN DISTINCT ROWS FOR THIS TO WORK!
-- e.g. query on left must produce identical results to query on the right:
-- ---------------------------------------+-----------------------------------
--           SELECT DISTINCT * FROM sigs  |  SELECT * FROM sigs
--           SELECT DISTINCT * FROM files |  SELECT * FROM files


-- Update *.java tallies.
UPDATE files SET uniqjavasigre = tally FROM (
  SELECT
    COUNT(DISTINCT sigs.sigsha1re) AS tally, infilesha1
  FROM
    files NATURAL JOIN sigs WHERE suffix = '.java'
  GROUP BY
    infilesha1
) t
WHERE files.filesha1 = t.infilesha1 ;


-- Update *.class tallies.
UPDATE files SET uniqclasssigre = tally FROM (
  SELECT
    COUNT(DISTINCT sigs.sigsha1re) AS tally, infilesha1
  FROM
    files NATURAL JOIN sigs WHERE suffix = '.class'
  GROUP BY
    infilesha1
) t
WHERE files.filesha1 = t.infilesha1 ;


