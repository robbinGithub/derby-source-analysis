/* Generated By:JavaCC: Do not edit this line. ijConstants.java */
package org.apache.derby.impl.tools.ij;

public interface ijConstants {

  int EOF = 0;
  int SINGLE_LINE_SQLCOMMENT = 7;
  int ABSOLUTE = 8;
  int AFTER = 9;
  int ALL = 10;
  int AS = 11;
  int ASYNC = 12;
  int ATTRIBUTES = 13;
  int AUTOCOMMIT = 14;
  int BANG = 15;
  int BEFORE = 16;
  int CLOSE = 17;
  int COMMIT = 18;
  int CONNECT = 19;
  int CONNECTION = 20;
  int CONNECTIONS = 21;
  int CURRENT = 22;
  int CURSOR = 23;
  int DISCONNECT = 24;
  int DRIVER = 25;
  int ELAPSEDTIME = 26;
  int END = 27;
  int EQUALS_OPERATOR = 28;
  int EXECUTE = 29;
  int EXIT = 30;
  int EXPECT = 31;
  int FAIL = 32;
  int FIRST = 33;
  int FOR = 34;
  int GET = 35;
  int GETCURRENTROWNUMBER = 36;
  int HOLD = 37;
  int HELP = 38;
  int INSENSITIVE = 39;
  int INTO = 40;
  int LAST = 41;
  int LOCALIZEDDISPLAY = 42;
  int MAXIMUMDISPLAYWIDTH = 43;
  int NAME = 44;
  int NEXT = 45;
  int NOHOLD = 46;
  int NOHOLDFORCONNECTION = 47;
  int OFF = 48;
  int ON = 49;
  int PASSWORD = 50;
  int PERIOD = 51;
  int PREPARE = 52;
  int PREVIOUS = 53;
  int PROCEDURE = 54;
  int PROPERTIES = 55;
  int PROTOCOL = 56;
  int QUIT = 57;
  int READONLY = 58;
  int RELATIVE = 59;
  int REMOVE = 60;
  int RESOURCE = 61;
  int ROLLBACK = 62;
  int RUN = 63;
  int TO = 64;
  int SAVEPOINT = 65;
  int SCROLL = 66;
  int SENSITIVE = 67;
  int SET = 68;
  int SHOW = 69;
  int SHUTDOWN = 70;
  int STATEMENT = 71;
  int USER = 72;
  int USING = 73;
  int WAIT = 74;
  int WITH = 75;
  int XA_1PHASE = 76;
  int XA_2PHASE = 77;
  int XA_DATASOURCE = 78;
  int XA_CONNECT = 79;
  int XA_COMMIT = 80;
  int XA_DISCONNECT = 81;
  int XA_END = 82;
  int XA_ENDRSCAN = 83;
  int XA_FAIL = 84;
  int XA_FORGET = 85;
  int XA_GETCONNECTION = 86;
  int XA_JOIN = 87;
  int XA_NOFLAGS = 88;
  int XA_PREPARE = 89;
  int XA_RECOVER = 90;
  int XA_RESUME = 91;
  int XA_ROLLBACK = 92;
  int XA_START = 93;
  int XA_STARTRSCAN = 94;
  int XA_SUCCESS = 95;
  int XA_SUSPEND = 96;
  int DATASOURCE = 97;
  int CP_DATASOURCE = 98;
  int CP_CONNECT = 99;
  int CP_GETCONNECTION = 100;
  int CP_DISCONNECT = 101;
  int WORK = 102;
  int COMMA = 103;
  int LEFT_PAREN = 104;
  int RIGHT_PAREN = 105;
  int DOUBLE_QUOTE = 106;
  int HASH = 107;
  int MINUS_SIGN = 108;
  int PLUS_SIGN = 109;
  int IDENTIFIER = 110;
  int LETTER = 111;
  int DIGIT = 112;
  int INTEGER = 113;
  int STRING = 114;

  int DEFAULT = 0;

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\r\\n\"",
    "\"\\n\"",
    "\"\\r\"",
    "\"\\f\"",
    "<SINGLE_LINE_SQLCOMMENT>",
    "\"absolute\"",
    "\"after\"",
    "\"all\"",
    "\"as\"",
    "\"async\"",
    "\"attributes\"",
    "\"autocommit\"",
    "\"!\"",
    "\"before\"",
    "\"close\"",
    "\"commit\"",
    "\"connect\"",
    "\"connection\"",
    "\"connections\"",
    "\"current\"",
    "\"cursor\"",
    "\"disconnect\"",
    "\"driver\"",
    "\"elapsedtime\"",
    "\"end\"",
    "\"=\"",
    "\"execute\"",
    "\"exit\"",
    "\"expect\"",
    "\"fail\"",
    "\"first\"",
    "\"for\"",
    "\"get\"",
    "\"getcurrentrownumber\"",
    "\"hold\"",
    "\"help\"",
    "\"insensitive\"",
    "\"into\"",
    "\"last\"",
    "\"localizeddisplay\"",
    "\"maximumdisplaywidth\"",
    "\"name\"",
    "\"next\"",
    "\"nohold\"",
    "\"noholdforconnection\"",
    "\"off\"",
    "\"on\"",
    "\"password\"",
    "\".\"",
    "\"prepare\"",
    "\"previous\"",
    "\"procedure\"",
    "\"properties\"",
    "\"protocol\"",
    "\"quit\"",
    "\"readonly\"",
    "\"relative\"",
    "\"remove\"",
    "\"resource\"",
    "\"rollback\"",
    "\"run\"",
    "\"to\"",
    "\"savepoint\"",
    "\"scroll\"",
    "\"sensitive\"",
    "\"set\"",
    "\"show\"",
    "\"shutdown\"",
    "\"statement\"",
    "\"user\"",
    "\"using\"",
    "\"wait\"",
    "\"with\"",
    "\"XA_1phase\"",
    "\"XA_2phase\"",
    "\"XA_datasource\"",
    "\"XA_connect\"",
    "\"XA_commit\"",
    "\"XA_disconnect\"",
    "\"XA_end\"",
    "\"XA_endrscan\"",
    "\"XA_fail\"",
    "\"XA_forget\"",
    "\"XA_getconnection\"",
    "\"XA_join\"",
    "\"XA_noflags\"",
    "\"XA_prepare\"",
    "\"XA_recover\"",
    "\"XA_resume\"",
    "\"XA_rollback\"",
    "\"XA_start\"",
    "\"XA_startrscan\"",
    "\"XA_success\"",
    "\"XA_suspend\"",
    "\"datasource\"",
    "\"CP_datasource\"",
    "\"CP_connect\"",
    "\"CP_getconnection\"",
    "\"CP_disconnect\"",
    "\"work\"",
    "\",\"",
    "\"(\"",
    "\")\"",
    "\"\\\"\"",
    "\"#\"",
    "\"-\"",
    "\"+\"",
    "<IDENTIFIER>",
    "<LETTER>",
    "<DIGIT>",
    "<INTEGER>",
    "<STRING>",
  };

}
