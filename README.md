# Airline Ticket Reservation System (ATRS)

![GitHub](https://img.shields.io/github/license/terasoluna-batch/v5-fw)

Airline Ticket Reservation System (ATRS) is a sample application for developers to learn how to make applications with TERASOLUNA Batch Framework for Java (5.x).

> **Note**
> [TERASOLUNA](https://terasoluna-batch.github.io/) is a customized framework based on [Macchinetta](https://macchinetta.github.io/) by NTT DATA.
> TERASOLUNA shares [ATRS published on Macchinetta]([GitHub - Macchinetta/atrs-batch](https://github.com/Macchinetta/atrs-batch)) as a sample application.
> Therefore, we did not change the license headers and package names from ATRS of Macchinetta in this ATRS of TERASOLUNA.

## Version mapping

The version mapping between TERASOLUNA and Macchinetta in ATRS is [here](https://github.com/terasoluna-batch/atrs-batch/wiki/ATRSのバージョンについて).

## How to run the application

### Install PostgreSQL

You need to create a database of PostgreSQL on a local machine. (PostgreSQL can be downloaded via [here site](http://www.postgresql.org/download/).)    

| Property      | Value      |
|:------------- |:---------- |
| database name | atrs_batch |
| user name     | postgres   |
| password      | postgres   |

### Insert test data

Execute the following command to initialize DB.

```console
$ cd C:\atrs-batch
$ mvn sql:execute
```

### Build

```console
$ mvn clean package
```

### Run sample jobs

* Update flight information

```console
$ scripts\JBBA01001.bat
```

- Export flight information

```console
$ scripts\JBBA02001.bat
```
