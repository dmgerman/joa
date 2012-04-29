default: result	
DB=maven

create:
	createdb $(DB)

tables: 
	printf "drop table sigs;create table sigs(filesha1 char(40), classname varchar(1024), sigsha1 char(40), losesigsha1 char(40),sigsha1re char(40), losesigsha1re char(40));" | psql $(DB)
	printf "drop table files; create table files(filename varchar(1024), level integer, basename varchar(255), path varchar(255), suffix varchar(100), infilesha1 char(40), filesha1 char(40),uniqJavaSigRe int, uniqClassSigRe int, java int, class int);" | psql $(DB)


insert: tables
	./insertSigs.pl $(DB) < /tmp/all.sigs 

result: insert
	printf "select distinct f1.filename, f2.filename as in from files f1 natural join sigs join files f2 on (f1.infilesha1 = f2.filesha1) where sigsha1re in (select sigsha1re from sigs natural join files where not (suffix = '.java') except select sigsha1re from files natural join sigs where (suffix = '.java')) order by filename;" | psql $(DB)
