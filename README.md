corewars_js - RISCV
===========


[![Build Status](https://travis-ci.com/westerndigitalcorporation/corewars-risc-v.svg?branch=master)](https://travis-ci.com/westerndigitalcorporation/corewars-risc-v)

This is a fork of corewars8086 Javascript engine + debugger that uses RISC-V assembly instead of x86.

 Getting Started
--------
 ### Prerequisites:
You will need to install Apache Ant - For windows, follow the instructions in here: https://www.mkyong.com/ant/how-to-install-apache-ant-on-windows/
 ### Installing
 First, to install `ivy` - the dependency manager for this project run the following:
```sh
ant bootstrap
```
 To build and test:
```sh
ant test
```
 To run:
```sh
ant devmode
```
and then select the `launch default browser` option.

 ## Licence
This project is licensed under the GPL v3.0 License - see the LICENSE file for details

 References:  
---------- 
https://codeguru.co.il/Xtreme/  
https://github.com/codeguru-il/corewars_riscv  
https://github.com/YoavKa/corewars_riscv    (disassembler)   
https://github.com/kimwalisch/calculator  (watch language)   


