#! /usr/bin/perl -w

use strict;

sub doMap($);

my $baseName = shift;
my $dir = shift;
if (!defined($dir)) {
  $dir = ".";
}
my @zoomLevels;
opendir(DIR, ".") || die "Could not open dir";
open(LIST, ">list.txt") || die "Could not open list";
my @dirEntries = readdir(DIR);
foreach my $d (@dirEntries) {
  if ($d =~ /^$baseName(\d+)$/) {
    @zoomLevels = (@zoomLevels, $1);
    my $zoom = $1;
    print "Zoom Level: " . $zoom . "\n";
    doMap($zoom);
  }
}
closedir(DIR);
close(LIST);
exit(0);

sub doMap($) {
  my ($zoomLevel) = @_;
  my $name2 = "$baseName${zoomLevel}000001";
  my $dir = "$baseName$zoomLevel/$name2/";
  print LIST $dir . "set/\n";
  
  open(IN, "<" . $dir . $name2 . ".map") || die "Could not open map file";
  my $wPixels;
  my $hPixels;
  my $longStart;
  my $longEnd;
  my $latStart;
  my $latEnd;
  while (<IN>) {
    if (/MMPXY,(\d+),(\d+),(\d+)/ && $1 == 4) {
      $wPixels = $2 + 1;
      $hPixels = $3 + 1;
    } elsif (/MMPLL,(\d+),([^,]+),([^,]+)\n/) { 
      if ($1 == 1) {
        $longStart = $2;
        $latStart = $3;
      }
      if ($1 == 4) {
        $longEnd = $2;
        $latEnd = $3;
      }
    }
  }

  close(IN);

  print $wPixels . ":" . $hPixels . " -> " . $longStart . ":" . $latStart . " to " . $longEnd . ":" . $latEnd . "\n";

  open(OUT, ">" . $dir . "set/map.txt") || die "Could not open map.txt";

  print OUT "baseFileName $name2\n";
  print OUT "longDiff " . ($longEnd - $longStart). "\n";
  print OUT "latDiff " . ($latStart - $latEnd) . "\n";
  print OUT "tileWidth 256\n";
  print OUT "tileHeight 256\n";
  print OUT "mapOffsetLong " . $longStart . "\n";
  print OUT "mapOffsetLat " . $latStart . "\n";
  print OUT "maxTileX " . $wPixels . "\n";
  print OUT "maxTileY " . $hPixels . "\n";

  close (OUT);
}