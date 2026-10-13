"use client";

import { useState } from 'react';
import { Sidebar } from './Sidebar';
import { Search, Bell, Settings, Command } from 'lucide-react';
import { motion } from 'framer-motion';

export function AppShell({ children }: { children: React.ReactNode }) {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  return (
    <div className="flex h-screen w-full bg-[#09090b] text-zinc-100 overflow-hidden">
      {/* Background ambient glow */}
      <div className="ambient-glow bg-indigo-500/20 w-[500px] h-[500px] -top-[250px] -left-[250px]" />
      
      <Sidebar isCollapsed={isSidebarCollapsed} onToggle={() => setIsSidebarCollapsed(!isSidebarCollapsed)} />
      
      <div className="flex-1 flex flex-col min-w-0 h-full relative z-10">
        {/* Top Header / Command Bar */}
        <header className="h-14 border-b border-white/5 bg-[#09090b]/80 backdrop-blur-xl flex items-center justify-between px-6 sticky top-0 z-20">
          <div className="flex items-center gap-4 flex-1">
            <div className="flex items-center gap-2 px-3 py-1.5 bg-white/5 border border-white/10 rounded-md text-sm text-zinc-400 w-full max-w-md cursor-pointer hover:bg-white/10 transition-colors">
              <Search className="w-4 h-4" />
              <span>Search projects, findings, or run commands...</span>
              <div className="ml-auto flex items-center gap-1 text-xs font-mono bg-white/10 px-1.5 py-0.5 rounded">
                <Command className="w-3 h-3" />
                <span>K</span>
              </div>
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            <button className="relative p-2 text-zinc-400 hover:text-white transition-colors">
              <Bell className="w-4 h-4" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-indigo-500 rounded-full border-2 border-[#09090b]"></span>
            </button>
            <div className="w-px h-4 bg-white/10"></div>
            <div className="flex items-center gap-2 cursor-pointer group">
              <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center text-xs font-bold shadow-lg shadow-indigo-500/20">
                YS
              </div>
            </div>
          </div>
        </header>
        
        {/* Main Content Area */}
        <main className="flex-1 overflow-y-auto overflow-x-hidden p-8 scroll-smooth">
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            className="max-w-7xl mx-auto"
          >
            {children}
          </motion.div>
        </main>
      </div>
    </div>
  );
}
