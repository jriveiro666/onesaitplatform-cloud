import Cluster from 'puppeteer-cluster';
import { toPDFTask, toImgTask } from './tasks.mjs';
import { TaskType, browsersPoolLengthMax } from '../conf/vars.mjs';
import logger from './logger.service.mjs';

var cluster = false;

async function initCluster(){
    cluster = await Cluster.Cluster.launch({
        concurrency: Cluster.Cluster.CONCURRENCY_CONTEXT,
        maxConcurrency: browsersPoolLengthMax,
        puppeteerOptions: {defaultViewport: null, headless: true,ignoreHTTPSErrors: true,args: ['--no-sandbox', '--disable-setuid-sandbox']}
    });
    logger.info("Cluster ready")
}

initCluster();

export async function addTaskToCluster(tasktype,params){
    var task;
    switch(tasktype){
        case TaskType.PDF:
            task = toPDFTask
            break;
        case TaskType.IMG:
            task = toImgTask
            break;
    }
    cluster.queue(params,task);
}