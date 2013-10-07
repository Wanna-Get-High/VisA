funcprot(0);

function basse = getCourbeBasse()
    basse(1,1:10) = 1;
    basse(1,11:19) = 2 - ((0.9:0.1) - (-1/10)*(11:19));
    basse(1,20:40) = 0;
endfunction

function moyenne = getCourbeMoyenne()
    moyenne(1,1:10) = 0;
    moyenne(1,11:20) = -1 + ((0.9:0.1) - (-1/10)*(11:20));
    moyenne(1,20:29) = 3 - ((0:0.9) - (-1/10)*(20:29));
    moyenne(1,30:40) = 0;
endfunction

function haute = getCourbeHaute()
    haute(1,1:20) = 0;
    haute(1,21:29) = -2 + ((0:0.9) - (-1/10)*(21:29));
    haute(1,30:40) = 1;
endfunction

function chauffeFort = getCourbeChauffe()
    chauffeFort(1,1:80) = 0;
    chauffeFort(1,81:99) = -0.3 + ((0.05:0.05:0.95) - (-1/300)*(81:99));
    chauffeFort(1,100:150) = 1;
endfunction

function minimum = getSousEnsOperateurMin(a,b)
    minimum = min(a,b);
endfunction

function maximum = getSousEnsOperateurMax(a,b)
    maximum = max(a,b);
endfunction

temperature = [1:40];


////////////////////////////////////////////////////////////
// 1) Fonction d'appartenance
////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////
// Question 1 : programme generant des ensembles flous
////////////////////////////////////////////////////////////
basse = getCourbeBasse();
moyenne = getCourbeMoyenne();
haute = getCourbeHaute();


//    affichage de la courbe : Partition floue de l univers du discours
    subplot(221);
    plot2d(temperature,basse,2);
    plot2d(temperature,moyenne,3);
    plot2d(temperature,haute,5);
    xtitle("Partition floue de l univers du discours","Temperature (˚C)","");

////////////////////////////////////////////////////////////
// Question 2 : degree d'appartenance aux differents sous-ensemble 
// pour une temperature de 16 degree
////////////////////////////////////////////////////////////
temp_16_deg_basse = basse(16);
temp_16_deg_moyenne = moyenne(16);
temp_16_deg_haute = haute(16);

//    affichage des degree d'appartenance
    disp(temp_16_deg_basse);
    disp(temp_16_deg_moyenne);
    disp(temp_16_deg_haute);


////////////////////////////////////////////////////////////
// Question 3 : ensemble flou "temperature basse ou moyenne"
////////////////////////////////////////////////////////////
temp_moy_ou_basse = getSousEnsOperateurMax(basse,moyenne);

//    affichage de la courbe : temperature moyenne ou basse
    subplot(222);
    plot2d(temperature,temp_moy_ou_basse,6);
    xtitle("Temperature Basse ou moyenne","Temperature (˚C)","");


////////////////////////////////////////////////////////////
// 2) Fonction d'appartenance
// generation ensemble flou : voir fonction 
//      getSousEnsOperateurMin et getSousEnsOperateurMax
////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////
// 3) implication floue
////////////////////////////////////////////////////////////
chauffeFort = getCourbeChauffe();

// on creer un tableau de taille 15 avec la valeur se trouvant à basse(12)
temp_mesure(1,1:150) = basse(12);

// ensemble flou issue de l'utilisation de l'impolication de Mamdani
r = getSousEnsOperateurMin(temp_mesure,chauffeFort);

// affichage de la courbe chauffer fort
    subplot(223);
    plot2d([1:150],chauffeFort,3);
    xtitle("Chauffer fort","Puissance chauffe (hW)","");

// affichage de la courbe : si temperature basse alors Chauffer fort
    subplot(224);
    plot2d([1:150],r,3,rect=[0,0,150,1]);
    xtitle("Ensemble flou de sortie pour : Si temperature basse alors Chauffer fort -> 12 degree","Puissance chauffe (hW)","Chauffer fort");



// redimentionnement de la fenetre
f= gcf();
f.figure_size=[1020,800];










