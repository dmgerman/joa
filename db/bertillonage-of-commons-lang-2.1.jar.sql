--
-- NOTE:  Must have run 'update_signature_tallies.sql' at least once beforehand.
--
-- NOTE:  files AND sigs MUST CONTAIN DISTINCT ROWS FOR THIS TO WORK!
-- e.g. query on left must produce identical results to query on the right:
-- ---------------------------------------+-----------------------------------
--           SELECT DISTINCT * FROM sigs  |  SELECT * FROM sigs
--           SELECT DISTINCT * FROM files |  SELECT * FROM files

SELECT DISTINCT
  70 AS "commons-lang-2.1.jar = a = 70",
  uniqclasssigre AS b,
  a_intersect_b,
  70 + uniqclasssigre - a_intersect_b AS a_union_b,
  TO_CHAR(1.0 * a_intersect_b / (70 + uniqclasssigre - a_intersect_b), '0.999') AS jaccard,
  TO_CHAR(1.0 * a_intersect_b /  70, '0.999') AS inclusion,
  basename || suffix AS artifact
FROM files f INNER JOIN (
  SELECT COUNT(DISTINCT sigsha1re) AS a_intersect_b, infilesha1 FROM files NATURAL JOIN sigs WHERE sigsha1re IN (
'03f431e389e9bdc337b1e78c0d2524b3f5c5c2b4', 'fa8842b520b3c1c8d566be7b0b773c05fd8f7966', '506931204df8b9878ed2e51bce73cd035cfa3400',
'ee073591b79ccaf53249ac1c09063ba7a957e943', 'dbd7f24ce2d9206d9ec2e3747923fdb4b5eb93ed', '1235afde0483e8cf960c457d0d2391054a23dc52',
'4a6c156725053ab1f7974e0c5aab7449627bf77b', '5c5c9a6ff9ab20f71d7a5e0d747bdf909719d0d7', '114eeaef4657dceb66023b289b932aae4ff3f862',
'046863b5f13c2b66a6c8a245afc75bbe165db938', 'a4fb0163735f37cfc875ac479a94f3b3c07f04c1', '604711bf9293895b6c9f984a4cc0bcd8d29b5bbd',
'e41a94be289e580e782f3f4c9f01660056d3b2b5', '70555c074dad96ddcd49e696262f6ff9b1d894ae', 'c190af523e132350b181a21050af3b86a030dbe3',
'a69d1aab62bb48442994e26ac14c9e6370cb07a2', '8a375ab44ccc4db22389c7a7bb4f7cec331890d9', 'f96d22ebdd6a6d323ce69ac55a96be02429b7a58',
'e7c8bda1457b83b6e5c2d6c07accc51384771f2e', 'fff0449bcce3b69b9924dc1d3672cd3f2668de0c', 'fe5c8c560834db187b5009c2be02500c46e351ca',
'df0d0a047efd8091115e7697e9473f94c95fbdfd', '1c93285773f7fe89295a6cfe1deac3cb77f3c48a', 'cd5ca0750281d2e3e7511fbf93cae9d010f3ec11',
'1eb85dd6889d3a99c57397af53ee208874f6cb56', '376e6b3a5b959d5c5592efcf6a1c5e3ba399bdd4', '40a5812177ddb4d735396297a149c9365e52db83',
'96ca4b2a829a739ebdbad60ba750ebbb66fe118c', '3a0f6a2f64db40b35abeb655bbed889cdbeb540b', '43259b0e8f0b1869e26bc0acb2d1223fe83146d6',
'25cc3778e4801014700d7560ddef8d957612592b', '81f7c5b78d456e945b10320170bf7fc45c1a8733', 'a5f711ef96d6d680ba11c3a228b9bc8bd041a87b',
'81e55b42b096c75ce0e6ebdb126f5417784e3c1c', '7a71d13b94e1151b8e3ce76f1453e45726a49cd1', 'e8639dec1bf6f122a3ce3d07f8f5ffc3b016b517',
'64baeacdc777399f0ea9c2736ddcd950da2d3206', 'b33c4ff6ceef7528e0e970db366b26c2fd94415f', 'cb9fd97c18baf344c9b439c25aeb29beea7e15a4',
'e6fe5a8e87a5b3632619afecc6f513d4f434a849', '7f1d20b0b64a92e8759b48cdff96377124bb5d43', 'c7c3af7bfb61882511c1cd62208d65f4bc390a24',
'a7c16a46bfcd54313b3d9f0c15d01f0a3b828d91', '98216ef6b3e3c0c8f4f546be342014a2fb7ccf1c', 'dea7ccf99e142cbff5f9d4f5b9a19c8fe2103862',
'3611bcbd3fca9c6c5834ff400e4affd9517c9ad4', '39c7ef71149ed3c71241bdd470b27da2f7c260ac', '580a29fae573f6c3d38eee1320e06c122ccfefb5',
'8162b5164926267f24dc839d9dd677dbf27239da', 'a313c2058a518fdb7cfd67e9916304bc5d1d4423', 'dbe49d1b46e1282b0be2876d46fe179fe8a99809',
'd858654ed2715d656bb667f50c844b65c1df241a', 'f3cc6be4486dfa851b1120635f2ac00a33923188', '084beb247edd0db60b7ff3d8b938630d435f27a2',
'dd472e8fe102d9fe652364ea787e92f5049851b8', 'e8364a6ca3fbb927f8e27989d910ae7204ce69e3', '6ce132f3cd37cc1c9e45f5c0309433df2e780df7',
'b1cf5b17cadf6232a03174fec62e2db6627e12e1', '196ca836fc0d95176a46180c23264b036e6b5871', 'e6581ec114774ab66dd3aeb2772f79a9021f289a',
'a2abb13a57cad34fcd8c6d4e1b9f1aada08d6a68', '35cc881c1958bdf57c92c5f689492dc7b1eee9f4', 'f810492a8578a9ec358688fa601e754246e0f570',
'42143db2167a1f90e993a2cd5759e218916e9e60', '4f6e65b23c8dc13c886c1b4134eac66f381d402f', 'd589b684ce30fba7a068439d83e4401916c8fa3f',
'49bcab6e94502fa7587d216e9aad0b8bfaed851d', 'e83d351b4ace3cdaa9e9e25c138169c0a8a554ef', '0012542503d64e2b46e0111a40de2887e409a7f2',
'6176f818a3d590cc5dc05e3ce6d82866fc909b14'
) GROUP BY infilesha1 ) t ON (f.filesha1 = t.infilesha1) WHERE f.uniqclasssigre > 0 ORDER BY jaccard DESC, inclusion DESC ;




/*

Results using the 1/35th test database dmg provided me:

 commons-lang-2.1.jar = a = 70 | b  | a_intersect_b | a_union_b | jaccard | inclusion |     basename     
-------------------------------+----+---------------+-----------+---------+-----------+------------------
                            70 | 70 |            70 |        70 |  1.000  |  1.000    | commons-lang-2.1
                            70 | 77 |             2 |       145 |  0.014  |  0.029    | commons-lang-2.3
                            70 | 77 |             2 |       145 |  0.014  |  0.029    | commons-lang-2.3
                            70 | 77 |             2 |       145 |  0.014  |  0.029    | commons-lang-2.3
                            70 | 77 |             2 |       145 |  0.014  |  0.029    | commons-lang-2.3
*/
