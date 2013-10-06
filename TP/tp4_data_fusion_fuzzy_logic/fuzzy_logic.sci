funcprot(0);



function e = drawUnivers()

endfunction


//drawUnivers();

    // creer un tableau 2 dimension 
    temperature = [1:40];

    basse(1,1:10) = 1;
    basse(1,11:19) = 2 - ((0.9:0.1) - (-1/10)*(11:19));
    basse(1,20:40) = 0;
    
    moyenne(1,1:20) = 0;
    moyenne(1,21:29) = -2 + ((0:0.9) - (-1/10)*(21:29));
    moyenne(1,30:40) = 1;
    
    haute(1,1:10) = 0;
    haute(1,11:20) = -1 + ((0.9:0.1) - (-1/10)*(11:20));
    haute(1,20:29) = 3 - ((0:0.9) - (-1/10)*(20:29));
    haute(1,30:40) = 0;
    
    //disp((-1/10)*[10:20]);
   // disp([1:-0.1:0]);
   // [1:-0.1:0]
    
//      coeff pente A = yb - ya / xb - xa
// A = 0-1 / 20 - 10
// 
//      pente y = Ax+B;
//      B = y - Ax;
    
   // disp(basse);
    
    plot2d(temperature,basse,2);
    plot2d(temperature,moyenne,3);
    plot2d(temperature,haute,5);
