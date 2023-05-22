#include <stdio.h>

const int y = 4;


struct asd {
	int me;
	char you;
	
	
	
	

};
int __dLang_asd_rrrr(struct asd *this) {
		printf("%d", this->me); //should compile
	}

char __dLang_asd_r2r2(struct asd *this) {
		printf("%c", this->you); //should also compile
	}


int main() {
	struct asd r;
	__dLang_asd_r2r2(&r);
	return 0;
}