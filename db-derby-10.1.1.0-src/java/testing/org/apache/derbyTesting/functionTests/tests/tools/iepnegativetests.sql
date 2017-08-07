--testing error cases for import/export
create schema iep;
create table iep.t1(a int);
insert into iep.t1 values(100) , (101) , (102) , (103) , (104) , (105) , (106);
--export error cases
--export can not create file
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'nodir/t1.dat' , 
                                 null, null, null) ;
--export table not found
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'NOTABLE' , 'extinout/t1.dat' , 
                                 null, null, null) ;
---export schema is not valid
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('XXXX', 'T1' , 'extinout/t1.dat' , 
                                 null, null, null) ;
--export query is invalid (syntax error)
call SYSCS_UTIL.SYSCS_EXPORT_QUERY('select from t1', 
                                    'extinout/t1.dat' , null, null, null) ;

--export codeset is invalid
call SYSCS_UTIL.SYSCS_EXPORT_QUERY('select * from iep.t1', 
                                    'extinout/t1.dat' , null, null, 'NOSUCHCODESET') ;

call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('XXXX', 'T1' , 'extinout/t1.dat' , 
                                 null, null, null) ;
--export delimiter errror cases
--period can not be used as character ot column delimiter
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 null, '.', null) ;
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 '.', null, null) ;

--same delimter can not be used as character and column delimters
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 ';', ';', null) ;

--space character can not be a delimiter
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 ' ', ';', null) ;

call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 null, ' ', null) ;

--if emtry strinng is passed actual value delimiter should be space
--and the that should become a invalid delimiter 
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 '', ';', null) ;

call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 null, '', null) ;


--more than one character passed to the delimiters get truncated to one
--following one should give error because eventually '\' delimiter 
--is used a both for char and col
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 '\a', '\', null) ;


--DO A VALID EXPORT AND  IMPORT 
set schema iep;
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 null, null, 'utf-8') ;
delete from t1 ;

call SYSCS_UTIL.SYSCS_IMPORT_TABLE('IEP', 'T1' , 'extinout/t1.dat' , 
                                 null, null, null, 0) ;
select * from t1;

--import error cases
--import can not find input file
call SYSCS_UTIL.SYSCS_IMPORT_TABLE('IEP', 'T1' , 'nodir/t1.dat' , 
                                 null, null, null, 0) ;
--import table not found
call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'NOTABLE' , 'extinout/t1.dat' , 
                                 null, null, null, 0) ;
--import schema is not valid
call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('XXXX', 'T1' , 'extinout/t1.dat' , 
                                 null, null, null, 0) ;

call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 null, null, 'INCORRECTCODESET', 0) ;

--check import with invalid delimiter usage
--if emtry strinng is passed actual value delimiter should be space
--and the that should become a invalid delimiter 
call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 '', ';', null, 0) ;

call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 null, '', null, 0) ;

--same delimter can not be used as character and column delimters
call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'T1' , 'extinout/t1.dat' , 
                                 ';', ';', null, 1) ;


autocommit off;
create table v1(a int) ;
declare global temporary table session.temp1(c1 int) on commit preserve rows not logged;
insert into session.temp1 values(1) , (2) , (3) , (4) , (5) , (6);
select * from session.temp1;
--export to from a temporary table
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('SESSION', 'TEMP1' , 'extinout/temp1.dat' , 
      
                           null, null, null) ;
-- because temporary table has on commit preserve rows, commit issued by export will not delete data from the temp table.
select * from session.temp1;
--import back to a regualr table
call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'V1' , 'extinout/temp1.dat' , 
                                 null, null, null, 0) ;
select * from v1;
commit;
--import to a temp table should fail with a table not found errror
declare global temporary table session.temp2(c1 int) not logged;
call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('SESSION', 'TEMP2' , 'extinout/temp1.dat' , 
                                 null, null, null, 0) ;

select * from session.temp2 ;
commit ;
drop table v1;
autocommit on;

create table t3(c1 int , c2 double , c3 decimal , c4 varchar(20) );
insert into t3 values(1 , 3.5 , 8.6 , 'test strings');
insert into t3 values(2 , 3.5 , 8.6 , 'test strings');
insert into t3 values(3 , 3.5 , 8.6 , 'test strings');
insert into t3 values(4 , 3.5 , 8.6 , 'test strings');
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T3' , 'extinout/t3.dat' , 
                                 null, null, null) ;

call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'T3' , 'extinout/t3.dat' , 
                                 null, null, null, 0) ;
select * from t3;

--import data column names are incorrect
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , 'X1, X2, X3, X4', null,
                                 'extinout/t3.dat' , 
                                 null, null, null, 0) ;

call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , 'X1, X2, X3', '1,2,3,4', 
                                 'extinout/t3.dat' , 
                                 null, null, null, 0) ;

--import data insert column names count < column indexes does not match
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , 'C1, C2, C3', '1,2,3,4', 
                                 'extinout/t3.dat' , 
                                 null, null, null, 0) ;
--import data column indexes count > insert columns count
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , 'C1, C2, C3,C4', '1,2', 
                                 'extinout/t3.dat' , 
                                 null, null, null, 0) ;

call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , null, '11,22,12,24', 
                                 'extinout/t3.dat' , 
				 null, null, null, 0) ;


--repeat the above type cases with empty file and minor variation to paramters
delete from t3 ;
call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'T3' , 'extinout/t3.dat' , 
                                  ';', '^', 'utf-16') ;
--import data column names are incorrect
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , 'X1, X2, X3, X4', null, 
                                 'extinout/t3.dat' , 
                                 ';', '^', 'utf-16', 1) ;

call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , 'X1, X2, X3', '1,2,3,4', 
                                 'extinout/t3.dat' , 
				    ';', '^', 'utf-16', 1) ;

--import data insert column names count < column indexes does not match
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , 'C1, C2, C3', null, 
                                 'extinout/t3.dat' , 
				    ';', '^', 'utf-16', 1) ;
--import data column indexes count > insert columns count
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , null, '1,2', 
                                 'extinout/t3.dat' , 
				    ';', '^', 'utf-16', 1) ;


--specify column indexes that are not there in the file that is being  imported
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'T3' , null, '11,22,12,24', 
                                 'extinout/t3.dat' , 
				    ';', '^', 'utf-16', 1) ;


--import to a system table shoud fail
call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('SYS', 'SYSTABLES' , 'extinout/t3.dat' , 
                                      ';', '^', 'utf-16', 1) ;


---not supported by db2 cloudscape import/export
create table ntype(a int , ct CLOB(1024));
create table ntype1(bt BLOB(1024) , a int);

call SYSCS_UTIL.SYSCS_EXPORT_TABLE ('IEP', 'NTYPE' , 'extinout/ntype.dat' , 
                                 null, null, null) ;
call SYSCS_UTIL.SYSCS_EXPORT_QUERY('select * from iep.ntype1',
				   'extinout/ntype.dat' , 
                                   null, null, null) ;

call SYSCS_UTIL.SYSCS_IMPORT_TABLE ('IEP', 'NTYPE' , 'extinout/ntype.dat' , 
                                 null, null, null, 0) ;
call SYSCS_UTIL.SYSCS_IMPORT_DATA('IEP', 'NTYPE1' , null , null, 
                                 'extinout/ntype.dat' , 
                                 null, null, null, 0) ;

--import should aquire a lock on the table
create table parent(a int not null primary key);
insert into parent values (1) , (2) , (3) , (4) ;
call SYSCS_UTIL.SYSCS_EXPORT_QUERY('select * from parent where a < 3' , 'extinout/parent.del' , 
                                 null, null, null) ;
connect 'jdbc:derby:wombat' as c1;
connect 'jdbc:derby:wombat' as c2;
set connection c1;
autocommit off;
lock table iep.parent in share mode;
set connection c2;
autocommit off;
--following import should fail with lock time out
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.locks.waitTimeout', '5');
CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE('IEP', 'PARENT', 'extinout/parent.del',null, null, null,1);
disconnect c1;
disconnect c2;

set connection connection0;

