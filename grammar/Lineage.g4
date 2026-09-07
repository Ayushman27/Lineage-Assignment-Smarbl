grammar Lineage;

// Parser rules

expr
    : expr op=('*' | '/') expr       # MulDiv
    | expr op=('+' | '-') expr       # AddSub
    | '(' expr ')'                   # Parens
    | ifExpr                         # IfElseExpr
    | IDENTIFIER                     # Var
    | NUMBER                         # Const
    ;

ifExpr
    : 'if' '(' condition ')' 'then' expr 'else' expr
    ;

condition
    : expr op=('==' | '!=' | '<' | '<=' | '>' | '>=') expr
    ;

// Lexer rules

IF      : 'if' ;
THEN    : 'then' ;
ELSE    : 'else' ;

NUMBER      : [0-9]+ ('.' [0-9]+)? ;
IDENTIFIER  : [a-zA-Z_][a-zA-Z_0-9]* ;

WS          : [ \t\r\n]+ -> skip
;
