import winston from 'winston';
import 'winston-daily-rotate-file';
import {appname, vendor, logdir} from '../conf/vars.mjs';

var transport = new (winston.transports.DailyRotateFile)({
    filename: vendor + "-" + appname + '-%DATE%.log',
    datePattern: 'YYYY-MM-DD-HH',
    zippedArchive: true,
    maxSize: '100m',
    maxFiles: '14d',
    dirname: logdir
});

const textformat = winston.format.printf(({ level, message, label, timestamp }) => {
    return `${timestamp} [${label}] ${level}: ${message}`;
});

var logger = winston.createLogger({
    format: winston.format.combine(
        winston.format.label({ label: appname }),
        winston.format.timestamp(),
        textformat
    ),
    transports: [
        new winston.transports.Console(),
        transport
    ]
});

export default logger;