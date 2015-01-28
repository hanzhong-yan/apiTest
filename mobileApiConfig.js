module.exports = {
    desc : '这个是mobile api 所提供的所有接口说明',
    data : { 
        2132 : {
                 acid:2132,
                 desc:'易宝支付请求',
                 param:{
                            uid : {required:1,default:102608},
                            totalFee : {required:1,default:10},
                            fromType : {required:0,default:1110},
                            imei : {required:1},
                            subject : {required:0}
                        }
             }
         } 
}
