import * as clusterservice from "./cluster.service.mjs";
import { TaskType } from "../conf/vars.mjs";
import logger from "./logger.service.mjs";

export async function imgController(request, response) {
    if(checkRequestParam(request)){
        logger.info('Cluster for img queue ' + request.query.url) 
        await clusterservice.addTaskToCluster(TaskType.IMG,{url:request.query.url,request:request,response:response});
        logger.info('Cluster for img queue end ' + request.query.url)
    }
}
	
export async function pdfController(request, response) {
    if(checkRequestParam(request)){
        logger.info('Cluster for PDF queue ' + request.query.url) 
        await clusterservice.addTaskToCluster(TaskType.PDF,{url:request.query.url,request:request,response:response});
        logger.info('Cluster for PDF queue end ' + request.query.url)
    }
}

function checkRequestParam(request){
    if(request.query.url){
        return true;
    }
    else {
        const error = "No url provided"  
        logger.error(error)
        response.status(400).send(error);
        return false;
    }
}
