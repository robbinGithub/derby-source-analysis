

-- test database encryption

-- for bug 3668 - you couldn't change the password without exiting out of db create session
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Thursday, Wednesday');

disconnect;
connect 'jdbc:derby:;shutdown=true';

-- test for bug 3668
-- try the old password, should fail
connect 'jdbc:derby:wombat;bootPassword=Thursday';
connect 'jdbc:derby:wombat;bootPassword=Wednesday';
-- switch back to old password
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Wednesday, Thursday');

create table t1 ( a char(20));

-- make sure we cannot access the secret key
values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('bootPassword');
values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('encryptedBootPassword');

insert into t1 values ('hello world');

-- change the secret key

-- these should fail
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', null);
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'wrongkey, ');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Thursday');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Thursday , ');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Thursday , short');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Thursdya , derbypwd');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Thursdayx , derbypwd');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'xThursday , derbypwd');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'thursday , derbypwd');

-- this should work
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', ' Thursday , Saturday');

-- this should fail
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Thursday , derbypwd');

-- change it again
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'Saturday,derbypwd');


-- make sure we cannot access the secret key
values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('bootPassword');
values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('encryptedBootPassword');

disconnect;
connect 'jdbc:derby:;shutdown=true';

connect 'jdbc:derby:wombat';
connect 'jdbc:derby:wombat;bootPassword=Thursday';
connect 'jdbc:derby:wombat;bootPassword=Saturday';
connect 'jdbc:derby:wombat;bootPassword=derbypwd';

values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('bootPassword');
values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('encryptedBootPassword');

-- change it again, make sure it trims white spaces
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', '   derbypwd   ,  bbderbypwdx  ');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('bootPassword', 'bbderbypwdx, derbypwdxx ');

values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('bootPassword');
values SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('encryptedBootPassword');

disconnect;
connect 'jdbc:derby:;shutdown=true';

connect 'jdbc:derby:wombat;bootPassword=derbypwd';
connect 'jdbc:derby:wombat;bootPassword=derbypwdxx';
select * from t1;

-- test that you cannot change the encryption provider or algorithm after database creation
-- this should not work
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('encryptionAlgorithm', 'DES/blabla/NoPadding');
call SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('encryptionProvider', 'com.pom.aplomb');






