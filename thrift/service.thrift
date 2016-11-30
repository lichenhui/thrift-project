namespace java cn.lichenhui.rpc.thrift.service

typedef i32 int
typedef i64 long

service IHelloService {
	int sum(1:int a, 2:int b),
	void ping()
}
service IDemoService {
	string echoHello(1:string msg),
	void ping()	
}
