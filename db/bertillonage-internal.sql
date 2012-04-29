--
-- Replace the two occurrences of '1a8b0f2e06e0c1f1ac23a7473a52f501529c9933' with the specific
-- internal jar you want to run 'Bertillonage' for.
--
-- FYI, '1a8b0f2e06e0c1f1ac23a7473a52f501529c9933' is checkstyle-3.3.jar.
-- (e.g., SELECT filename FROM files WHERE filesha1 = '1a8b0f2e06e0c1f1ac23a7473a52f501529c9933' ;)
--
--
--
-- NOTE:  Must have run 'update_signature_tallies.sql' at least once beforehand.
--
-- NOTE:  files AND sigs MUST CONTAIN DISTINCT ROWS FOR THIS TO WORK!
-- e.g. query on left must produce identical results to query on the right:
-- ---------------------------------------+-----------------------------------
--           SELECT DISTINCT * FROM sigs  |  SELECT * FROM sigs
--           SELECT DISTINCT * FROM files |  SELECT * FROM files

SELECT DISTINCT
  a,
  uniqclasssigre AS b,
  a_intersect_b,
  a + uniqclasssigre - a_intersect_b AS a_union_b,
  TO_CHAR(1.0 * a_intersect_b / (a + uniqclasssigre - a_intersect_b), '0.999') AS jaccard,
  TO_CHAR(1.0 * a_intersect_b /  a, '0.999') AS inclusion,
  basename || suffix AS artifact
FROM

  -- This should return a single cell, our 'a', hence the full join with the other tables.
  (SELECT COUNT(DISTINCT sigsha1re) AS a FROM sigs NATURAL JOIN files WHERE infilesha1 IN ('17198c393f6efcab9a22ea40c259634023d7f33a')) tally,

  -- Here is where we match signatures.
  files f INNER JOIN (

    SELECT
      count(DISTINCT s1.sigsha1re) AS a_intersect_b, f2.infilesha1
    FROM
      ((files f1 INNER JOIN  sigs s1 ON (f1.infilesha1 = '17198c393f6efcab9a22ea40c259634023d7f33a' AND f1.filesha1 = s1.filesha1))
                 INNER JOIN  sigs s2 ON (s1.sigsha1re = s2.sigsha1re))
                 INNER JOIN files f2 ON (s2.filesha1 = f2.filesha1)
    GROUP BY f2.infilesha1 

  ) t
ON (f.filesha1 = t.infilesha1) WHERE f.uniqclasssigre > 0 ORDER BY jaccard DESC, inclusion DESC ;
 




/*

Results using the 1/35th test database dmg provided me:

Neat... sources are matching, too, but since uniqclasssigre is NULL for them,
the appear earlier in the results.


  a  |  b  | a_intersect_b | a_union_b | jaccard | inclusion |           basename            
-----+-----+---------------+-----------+---------+-----------+-------------------------------
 211 |     |           135 |           |         |  0.640    | checkstyle-4.0-sources
 211 |     |           135 |           |         |  0.640    | checkstyle-4.1-sources
 211 |     |           119 |           |         |  0.564    | checkstyle-4.4-sources
 211 |     |            75 |           |         |  0.355    | checkstyle-5.0-beta01-sources
 211 | 211 |           211 |       211 |  1.000  |  1.000    | checkstyle-3.3
 211 | 208 |           191 |       228 |  0.838  |  0.905    | checkstyle-3.2
 211 | 233 |           186 |       258 |  0.721  |  0.882    | checkstyle-3.4
 211 | 238 |           159 |       290 |  0.548  |  0.754    | checkstyle-4.0-beta1
 211 | 239 |           153 |       297 |  0.515  |  0.725    | checkstyle-4.0-beta3
 211 | 239 |           153 |       297 |  0.515  |  0.725    | checkstyle-4.0-beta4
 211 | 248 |           139 |       320 |  0.434  |  0.659    | checkstyle-4.0-beta6
 211 | 248 |           139 |       320 |  0.434  |  0.659    | checkstyle-4.0-beta5
 211 | 249 |           135 |       325 |  0.415  |  0.640    | checkstyle-4.0
 211 | 249 |           135 |       325 |  0.415  |  0.640    | checkstyle-4.1
 211 | 250 |           120 |       341 |  0.352  |  0.569    | checkstyle-4.2
 211 | 256 |           119 |       348 |  0.342  |  0.564    | checkstyle-4.3
 211 | 256 |           119 |       348 |  0.342  |  0.564    | checkstyle-4.4
 211 | 159 |            63 |       307 |  0.205  |  0.299    | checkstyle-3.1
 211 | 266 |            75 |       402 |  0.187  |  0.355    | checkstyle-5.0-beta01
 211 | 270 |            69 |       412 |  0.167  |  0.327    | checkstyle-5.0
 211 | 114 |            34 |       291 |  0.117  |  0.161    | checkstyle-3.0

*/
