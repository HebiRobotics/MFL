
%% generate a curly d (U+0221) in UTF 8, 16, and 32
val.utf8 = native2unicode(hex2dec(['F0'; 'A0'; '9C'; '8E'])',  'UTF-8');
val.utf16 = native2unicode(hex2dec(['D8'; '41'; 'DF'; '0E'])', 'UTF-16');
val.utf32 = native2unicode(hex2dec(['00'; '02'; '07'; '0E'])', 'UTF-32');

%% save it out to disk
save('utf.mat', 'val')
