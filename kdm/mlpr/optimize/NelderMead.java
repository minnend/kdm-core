package kdm.mlpr.optimize;

/** multidim downhill simplex method; from NR 10.4 */
public class NelderMead
{
   public static final double TINY = 1.0e-10; // A small number
   public static final int NMAX = 5000; // Maximum allowed number of function evaluations
   
   /*public final static double GET_PSUM(double[][] p)
   {
      for (j=1;j<=ndim;j++){
         for (sum=0.0,i=1;i<=mpts;i++) sum += p[i][j];
         psum[j]=sum;
      }
   }*/
   
   /**
    * Multidimensional minimization of the function funk(x) where x[1..ndim] is a vector in ndim dimensions,
    * by the downhill simplex method of Nelder and Mead. The matrix p[1..ndim+1] [1..ndim] is input. Its
    * ndim+1 rows are ndim-dimensional vectors which are the vertices of the starting simplex. Also input is
    * the vector y[1..ndim+1], whose components must be preinitialized to the values of funk evaluated at the
    * ndim+1 vertices (rows) of p; and ftol the fractional convergence tolerance to be achieved in the
    * function value (n.b.!). On output, p and y will have been reset to ndim+1 new points all within ftol of
    * a minimum function value, and nfunk gives the number of function evaluations taken.
    */
/*   public static final void amoeba(double[][] p, double y[], int ndim, double ftol,
   double (*funk)(double []), int *nfunk)
   {
   double amotry(double **p, double y[], double psum[], int ndim,
   double (*funk)(double []), int ihi, double fac);
   int i,ihi,ilo,inhi,j,mpts=ndim+1;
   double rtol,sum,swap,ysave,ytry,*psum;
   psum=vector(1,ndim);
   *nfunk=0;
   GET_PSUM
   for (;;) {
   ilo=1;
   First we must determine which point is the highest (worst), next-highest, and lowest
   (best), by looping over the points in the simplex.
   ihi = y[1]>y[2] ? (inhi=2,1) : (inhi=1,2);
   for (i=1;i<=mpts;i++) {
   if (y[i] <= y[ilo]) ilo=i;
   if (y[i] > y[ihi]) {
   inhi=ihi;
   ihi=i;
   } else if (y[i] > y[inhi] && i != ihi) inhi=i;
   }
   rtol=2.0*fabs(y[ihi]-y[ilo])/(fabs(y[ihi])+fabs(y[ilo])+TINY);
   Compute the fractional range from highest to lowest and return if satisfactory.
   if (rtol < ftol) { If returning, put best point and value in slot 1.
   SWAP(y[1],y[ilo])
   for (i=1;i<=ndim;i++) SWAP(p[1][i],p[ilo][i])
   break;
   }
   if (*nfunk >= NMAX) nrerror("NMAX exceeded");
   *nfunk += 2;
   Begin a new iteration. First extrapolate by a factor −1 through the face of the simplex
   across from the high point, i.e., reflect the simplex from the high point.
   ytry=amotry(p,y,psum,ndim,funk,ihi,-1.0);
   if (ytry <= y[ilo])
   Gives a result better than the best point, so try an additional extrapolation by a
   factor 2.
   ytry=amotry(p,y,psum,ndim,funk,ihi,2.0);
   else if (ytry >= y[inhi]) {
   The reflected point is worse than the second-highest, so look for an intermediate
   lower point, i.e., do a one-dimensional contraction.
   ysave=y[ihi];
   ytry=amotry(p,y,psum,ndim,funk,ihi,0.5);
   if (ytry >= ysave) { Can’t seem to get rid of that high point. Better
   for (i=1;i<=mpts;i++) { contract around the lowest (best) point.
   412 Chapter 10. Minimization or Maximization of Functions
   Sample page from NUMERICAL RECIPES IN C: THE ART OF SCIENTIFIC COMPUTING (ISBN 0-521-43108-5)
   Copyright (C) 1988-1992 by Cambridge University Press. Programs Copyright (C) 1988-1992 by Numerical Recipes Software.
   Permission is granted for internet users to make one paper copy for their own personal use. Further reproduction, or any copying of machinereadable
   files (including this one) to any server computer, is strictly prohibited. To order Numerical Recipes books or CDROMs, visit website
   http:// www.nr.com or call 1-800-872-7423 (North America only), or send email to
         // directcustserv@cambridge.org (outside North America).
   if (i != ilo) {
   for (j=1;j<=ndim;j++)
   p[i][j]=psum[j]=0.5*(p[i][j]+p[ilo][j]);
   y[i]=(*funk)(psum);
   }
   }
   *nfunk += ndim; Keep track of function evaluations.
   GET_PSUM Recompute psum.
   }
   } else --(*nfunk); Correct the evaluation count.
   } Go back for the test of doneness and the next
   free_vector(psum,1,ndim); iteration.
   }
   #include "nrutil.h"
   double amotry(double **p, double y[], double psum[], int ndim,
   double (*funk)(double []), int ihi, double fac)
   Extrapolates by a factor fac through the face of the simplex across from the high point, tries
   it, and replaces the high point if the new point is better.
   {
   int j;
   double fac1,fac2,ytry,*ptry;
   ptry=vector(1,ndim);
   fac1=(1.0-fac)/ndim;
   fac2=fac1-fac;
   for (j=1;j<=ndim;j++) ptry[j]=psum[j]*fac1-p[ihi][j]*fac2;
   ytry=(*funk)(ptry); Evaluate the function at the trial point.
   if (ytry < y[ihi]) { If it’s better than the highest, then replace the highest.
   y[ihi]=ytry;
   for (j=1;j<=ndim;j++) {
   psum[j] += ptry[j]-p[ihi][j];
   p[ihi][j]=ptry[j];
   }
   }
   free_vector(ptry,1,ndim);
   return ytry;
   }
   */
}
