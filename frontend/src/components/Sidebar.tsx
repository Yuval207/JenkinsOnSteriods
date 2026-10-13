"use client";

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { motion } from 'framer-motion';
import {
  LayoutDashboard,
  FolderGit2,
  ShieldAlert,
  Activity,
  Settings,
  ShieldCheck,
  ChevronLeft,
  ChevronRight,
  Code2
} from 'lucide-react';
import { cn } from '@/lib/utils';

const navItems = [
  { name: 'Overview', href: '/', icon: LayoutDashboard },
  { name: 'Projects', href: '/projects', icon: FolderGit2 },
  { name: 'Findings', href: '/findings', icon: ShieldAlert },
  { name: 'Scans', href: '/scans', icon: Activity },
  { name: 'Policies', href: '/policies', icon: ShieldCheck },
  { name: 'Integrations', href: '/integrations', icon: Code2 },
  { name: 'Settings', href: '/settings', icon: Settings },
];

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
}

export function Sidebar({ isCollapsed, onToggle }: SidebarProps) {
  const pathname = usePathname();

  return (
    <motion.aside 
      initial={false}
      animate={{ width: isCollapsed ? 72 : 256 }}
      className="flex flex-col h-full bg-[#0c0c0f] border-r border-white/5 relative z-20 shadow-2xl shrink-0"
    >
      {/* Brand */}
      <div className="h-14 flex items-center px-4 border-b border-white/5 shrink-0">
        <Link href="/" className="flex items-center gap-3 overflow-hidden whitespace-nowrap">
          <div className="w-8 h-8 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center shrink-0">
            <ShieldCheck className="w-5 h-5 text-indigo-400" />
          </div>
          {!isCollapsed && (
            <motion.span 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="text-lg font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white to-zinc-400"
            >
              SystemGuard
            </motion.span>
          )}
        </Link>
      </div>
      
      {/* Navigation */}
      <div className="flex-1 overflow-y-auto py-6 px-3 flex flex-col gap-1">
        <div className="text-xs font-semibold text-zinc-500 uppercase tracking-wider mb-2 px-3">
          {!isCollapsed ? 'Platform' : '---'}
        </div>
        {navItems.map((item) => {
          const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));
          
          return (
            <Link
              key={item.name}
              href={item.href}
              className="relative outline-none"
              title={isCollapsed ? item.name : undefined}
            >
              {isActive && (
                <motion.div 
                  layoutId="activeNavIndicator"
                  className="absolute inset-0 bg-indigo-500/10 rounded-md border border-indigo-500/20"
                  transition={{ type: "spring", stiffness: 300, damping: 30 }}
                />
              )}
              <div className={cn(
                "relative z-10 flex items-center gap-3 px-3 py-2 rounded-md transition-colors text-sm font-medium",
                isActive ? "text-indigo-300" : "text-zinc-400 hover:text-zinc-100 hover:bg-white/5"
              )}>
                <item.icon className={cn("w-4 h-4 shrink-0", isActive ? "text-indigo-400" : "")} />
                {!isCollapsed && (
                  <span className="truncate">{item.name}</span>
                )}
              </div>
            </Link>
          );
        })}
      </div>
      
      {/* Collapse Toggle */}
      <div className="p-3 border-t border-white/5 shrink-0">
        <button 
          onClick={onToggle}
          className="w-full flex items-center justify-center p-2 text-zinc-500 hover:text-zinc-300 hover:bg-white/5 rounded-md transition-colors"
        >
          {isCollapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>
    </motion.aside>
  );
}
