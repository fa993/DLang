#include <stdio.h>

const int y = 4;


struct asd {
	int me;
	char you;
	
	int rrrr() {
		printf("%d", this->me); //should compile
	}
	
	char r2r2() {
		printf("%c", this->you); //should also compile
	}

};

int main() {
	struct asd r;
	r.r2r2();
	return 0;
}