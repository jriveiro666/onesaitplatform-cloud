import { pdf_landscape, maxtime, pdf_maxheight, pdf_maxwidth, pdf_defaultheight, pdf_defaultwidth, img_defaultheight,img_defaultwidth, img_maxheight, img_maxwidth } from "../conf/vars.mjs";
import logger from "./logger.service.mjs";

export async function toImgTask({ page, data: data }) {
    var request = data.request;
    var response = data.response;
    await fromRequestToImg(request, response, page);
}
export async function toPDFTask({ page, data: data }) {
    var request = data.request;
    var response = data.response;
    await fromRequestToPDF(request, response, page);
}
 
async function fromRequestToPDF (request,response,page){
    logger.info("Init pdf task");
    const waitfor = parseInt(request.query.waittime) || maxtime
    var pdfwidth = parseInt(request.query.width);
    var pdfheight = parseInt(request.query.height);
    var landscape = Boolean(request.query.pdflandscape) || pdf_landscape;
    if(request.query.oauthtoken){
        request.query.url += "?oauthtoken=" + request.query.oauthtoken
    }
    if(pdfwidth && pdfheight){
        logger.info(pdfwidth);
        logger.info(pdf_maxwidth);
        logger.info(pdf_maxheight);
        if(pdfwidth > pdf_maxwidth){
            pdfwidth = pdf_maxwidth;
            logger.warn("Param width over limit, set to max value " + maxwidth + " - " + request.query.url);
        }
        if(pdfheight > pdf_maxheight){
            pdfheight = pdf_maxheight;
            logger.warn("Param height over limit, set to max value " + pdf_maxheight + " - " + request.query.url);
        }
    }
    else{
        logger.warn("No query param pdfheight or pdfwidth found, set to default " + pdf_defaultwidth + "X" + pdf_defaultheight + " - " + request.query.url)
        pdfwidth = pdf_defaultwidth
        pdfheight = pdf_defaultheight
    }
    try{
        await page.setViewport({ width: pdfwidth, height: pdfheight })
        logger.info("goto " + request.query.url)
        await page.goto(request.query.url)
        logger.info("wait for " + waitfor + " " + request.query.url);
        await page.emulateMedia('screen');
        await page.waitFor(waitfor)
        await response.contentType('application/pdf');
        logger.info("pdf screenshot " + request.query.url + ", landscape: " + landscape + ", pdfwidth: " + pdfwidth + ", pdfheight: " + pdfheight);
        await page.evaluate(() => { 
            var elements = document.querySelectorAll(".md-menu");
            for(var i=0; i< elements.length; i++){
                elements[i].style.display = 'none';
            }
        });
        logger.info("h::" + pdfheight);
        logger.info("w::" + pdfwidth);
        response.send(await page.pdf({
            printBackground: true,
            landscape: landscape,
            height: pdfheight,
            width: pdfwidth
        }));
    }
    catch(error) {
        logger.error(error + " " +request.query.url);
        response.send("Error processing page: " + error)
    }
    if(page){
        page.close()
    }
    logger.info("finished " + request.query.url)
}


async function fromRequestToImg (request,response,page){
    logger.info("Init img task");
    const waitfor = parseInt(request.query.waittime) || maxtime
    var imgwidth = parseInt(request.query.width);
    var imgheight = parseInt(request.query.height);
    var fullpage = request.query.fullpage || false;
    if(request.query.oauthtoken){
        request.query.url += "?oauthtoken=" + request.query.oauthtoken;
    }
    if(imgwidth && imgheight){
        if(imgwidth > img_maxwidth){
            imgwidth = img_maxwidth
            logger.warn("Param width over limit, set to max value " + img_maxwidth + " - " + request.query.url)
        }
        if(imgheight > img_maxheight){
            imgheight = img_maxheight
            logger.warn("Param height over limit, set to max value " + img_maxheight + " - " + request.query.url)
        }
    }
    else{
        logger.warn("No query param imgheight or imgwidth found, set to default " + img_defaultwidth + "X" + img_defaultheight + " - " + request.query.url)
        imgwidth = img_defaultwidth
        imgheight = img_defaultheight
    }
    try{
        logger.info("Init viewport at " + imgwidth + "X" + imgheight);
        await page.setViewport({ width: imgwidth, height: imgheight });
        logger.info("goto " + request.query.url);
        await page.goto(request.query.url);
        logger.info("wait for " + waitfor + " " + request.query.url);
        await page.waitFor(waitfor);
        await response.contentType('image/png');
        logger.info("img screenshot " + request.query.url + ", imgwidth: " + imgwidth + ", imgheight: " + imgheight);
        await page.evaluate(() => { 
            var elements = document.querySelectorAll(".md-menu");
            for(var i=0; i< elements.length; i++){
                elements[i].style.display = 'none';
            }
        });
        response.send(await page.screenshot({fullPage:fullpage}));
    }
    catch(error) {
        logger.error(error);
        response.send("Error processing page: " + request.query.url)
    }
    if(page){
        page.close()
    }
    logger.info("finish " + request.query.url)
}