var http = require('http');
var fs = require("fs");
var util = require("util");

var count = 0;
var apis = require('./mobileApi.js').apis;

var express = require("express");
var app = express();

app.use(function(req,res,next){
   console.log("access:" + (++count));
   next();
});

app.get('/',function(req,res){
    res.writeHead(200, {'Content-Type': 'text/html'});
    fs.readFile("./front/index.html",'utf8',function(err,data){
        if(err) throw err;
        res.end(data);
    });
});

app.get('/getApis',function(req,res){
   res.send(util.format('%j',apis)); 
   res.end();
});

//http.createServer(function (req, res) {
//    console.log("access:" + (++count));
//    res.writeHead(200, {'Content-Type': 'text/html'});
//    fs.readFile("./front/index.html",'utf8',function(err,data){
//        if(err) throw err;
//        res.end(data);
//    });
//    //res.end('Hello World\n');
//}).listen(1337, '127.0.0.1');
app.listen(1337,function(){
  console.log("Server running at http://127.0.0.1:1337/");
});
//console.log('Server running at http://127.0.0.1:1337/');
