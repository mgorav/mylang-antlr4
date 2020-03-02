# MyLang (Python like) ANTLR 4

ANTLR is a  parser generator, or, sometimes called a compiler-compiler. In a nutshell: given a grammar of a language, ANTLR can generate a lexer and parser for said language.
The language we're going to interpret is called MyLang Language. t will be able to do a bit more than the most basic mathematical expression parsers, that supports arithmetic operators like +, -, * etc., and variable assignments. It will be able to parse and evaluate boolean and numerical expression. MyLang will also be able to define functions, recursively call these functions, use control statements like for and while and it will support a list data type, to name just a few features.

The BNF grammar is mentioned in file MyLang.g4


## Setting up

Clone the repository

```bash
git clone https://github.com/mgorav/mylang-antlr4.git
cd mylang-antlr4
```

Now generate the lexer, parser and visitor classes using the maven antlr4 plugin:

```bash
mvn antlr4:antlr4
```

Compile all classes:

```bash
mvn install
```

Now run `MyLang.mylang` to test the language which contains code in written in MyLang :-)

```bash
mvn exec:java
```

or running the java class MyLangExecutor.

