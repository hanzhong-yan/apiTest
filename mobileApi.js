//测试mobile api
var fs = require("fs");
var apiConfig = require("./mobileApiConfig.js");
var configFileData = {}; 
var apis = {};//在内存中存放所有的接口数据
/**
 * 初始化api信息
 */
function initApiConfig(){
    fs.readFile('./DispatcherService.java',{encoding:'utf8'},function(err,data){
        if(err) throw err;
        apis = extractApiInfo(data);
        persistApis();
    });
}

/**
 *持久化apis，暂时保存到文件中
 */
function persistApis(){
    //var apiJson = Json.stringify(apis);
    var util = require("util");
    //var apiJson = util.format("exports.apis=%j",apis);
    var apiJson = util.inspect(apis,{depth:null});
    apiJson = util.format("module.exports=%s",apiJson);
    fs.writeFile('api.js',apiJson,function(){
        console.log('the apis had wrote into api.json.');
    });
}

/*
 *从DispatcherService.java中提取api信息 
 *
 */
function extractApiInfo(data){
   //var pattern = /(['"])([^\1]*)\1/igm; 
   var pattern = /case\s+(\d{4}):\s*\/\/(.+)/mg;
   var result; 
   var commandCfg = require('./commandCfg.js').cfg;//以前维护的一个用来测试的api配置
   var apiCfg = require('./mobileApiConfig.js').data;//手动维护的api配置
   while(result = pattern.exec(data)){
       apis[result[1]] = {desc:result[2],param:commandCfg[result[1]] != null ? commandCfg[result[1]].data : null};
       if(apiCfg[result[1]]) {
           apis[result[1]] = apiCfg[result[1]];
       }
   }
   console.log(apis);
   return apis; 
}

//initApiConfig();

function loadApis(){
    apis = require('./api.js');
    //console.log(apis);
}

function saveApi(api){
    if(apis[api.id]){
        apis[api.id].param = api.param;
        persistApis();         
    }
}

loadApis();

exports.apis = apis;

//var repl = require("repl");
//var replServer = repl.start({
//    prompt : "node via stdin>",
//    input : process.stdin,
//    output : process.stdout
//});
//replServer.context.initApiConfig = initApiConfig;
//replServer.context.apiConfig = apiConfig;
//replServer.context.configFileData = configFileData;
