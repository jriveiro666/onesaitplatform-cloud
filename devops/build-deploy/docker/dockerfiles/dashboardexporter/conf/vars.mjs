//APP Name
export const appname = "dashboard-exporter";
export const vendor = "onesaitplatform";

//Server
export const port = 20300
export const host = "0.0.0.0"
export const logdir = "/var/log/dashboardexporter"

//Puppeteer Cluster
export const maxtime = parseInt(process.env.MAX_WAIT_TIME) || 60000
export const browsersPoolLengthMax = parseInt(process.env.BROWSERSPOOL_MAX) || 10

//IMG
export const img_maxwidth = parseInt(process.env.MAX_IMAGE_WIDTH) || 8000
export const img_maxheight = parseInt(process.env.MAX_IMAGE_HEIGHT) || 15000
export const img_defaultwidth = parseInt(process.env.DEFAULT_IMAGE_WIDTH) || 800
export const img_defaultheight = parseInt(process.env.DEFAULT_IMAGE_HEGIHT) || 600

//PDF
export const pdf_maxwidth = parseInt(process.env.MAX_PDF_WIDTH) || 8000
export const pdf_maxheight = parseInt(process.env.MAX_PDF_HEIGHT) || 15000
export const pdf_defaultwidth = parseInt(process.env.DEFAULT_PDF_WIDTH) || 800
export const pdf_defaultheight = parseInt(process.env.DEFAULT_PDF_HEGIHT) || 600
export const pdf_landscape = Boolean(process.env.PDF_LANDSCAPE) || false

export const TaskType = {
    PDF: 'pdf',
    IMG: 'img'
}
console.info("#############################");
console.info("#############################");
console.info("Default Server Vars: ");
console.info("appname              : " + appname              );
console.info("port                 : " + port                 );
console.info("host                 : " + host                 );
console.info("maxtime              : " + maxtime              );
console.info("browsersPoolLengthMax: " + browsersPoolLengthMax);
console.info("img_maxwidth         : " + img_maxwidth         );
console.info("img_maxheight        : " + img_maxheight        );
console.info("img_defaultwidth     : " + img_defaultwidth     );
console.info("img_defaultheight    : " + img_defaultheight    );
console.info("pdf_maxwidth         : " + pdf_maxwidth         );
console.info("pdf_maxheight        : " + pdf_maxheight        );
console.info("pdf_defaultwidth     : " + pdf_defaultwidth     );
console.info("pdf_defaultheight    : " + pdf_defaultheight    );
console.info("pdf_landscape        : " + pdf_landscape        );
console.info("#############################");
console.info("#############################");