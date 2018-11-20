corewars_js - RISC V
===========

This is a fork of corewars8086_js that replaces the x86 assembly with RISC-V assembly.

Getting Started
--------

### Prerequisites:
You will need to install Apache Ant - follow the instructions in here: https://www.mkyong.com/ant/how-to-install-apache-ant-on-windows/

### Installing

First, install `ivy` - the dependency manager for this project.
```sh
ant bootstrap
```

To build and run:
```sh
ant devmode
```
and then select the `launch default browser` option.

To run the tests:
```sh
ant test
```

## Licence
This project is licensed under the MIT License - see the LICENSE file for details

References:  
----------
https://codeguru.co.il/Xtreme/  
https://github.com/codeguru-il/corewars8086  
https://github.com/YoavKa/corewars8086    (disassembler)   
https://github.com/kimwalisch/calculator  (watch language)   


