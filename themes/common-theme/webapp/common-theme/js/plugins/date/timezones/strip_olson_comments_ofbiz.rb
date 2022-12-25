#!/usr/bin/ruby
 #
 # Copyright 2009 Matthew Eernisse (mde@fleegix.org)
 #
 # Licensed under the Apache License, Version 2.0 (the "License");
 # you may not use this file except in compliance with the License.
 # You may obtain a copy of the License at
 #
 #   http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing, software
 # distributed under the License is distributed on an "AS IS" BASIS,
 # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 # See the License for the specific language governing permissions and
 # limitations under the License.
 #
 # This is a sample script for stripping the copious comments
 # from Olson timezone data files.
 #
 
 # fully modified by jleroux for Apache OFBiz, done on Windows, hence the separator
 
if ARGV.length == 0
  print "Usage: strip_comments.rb /path/to/input/file\n"
  exit
else
  path = ARGV[0]
end

separator = "\\"

minDir = path + separator  + "min" + separator 

puts "========= delete files"

Dir.foreach(minDir) do  |f| 
    fn = File.join(minDir, f); 
    puts fn
    File.delete(fn) if f != '.' && f != '..'
end    

puts "========= create files"

Dir.foreach(path) do |item|
    next if item == '.' or item == '..' or item == 'min'
    puts "==== new file"
    puts "==== strip original file: " + path + separator  + item
    t= File.read(path + separator  + item) 
    t.gsub!(/^#.*\n/, '')
    t.gsub!(/^\n/, '')    
    outFile = File.new(minDir + item, "a")
    puts "==== create new file in min dir: " + minDir + item
    puts ""
    outFile.puts(t)
    outFile.close    
end


